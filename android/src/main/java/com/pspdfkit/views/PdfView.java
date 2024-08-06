/*
 * PdfView.java
 *
 *   PSPDFKit
 *
 *   Copyright © 2021-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   THIS SOURCE CODE AND ANY ACCOMPANYING DOCUMENTATION ARE PROTECTED BY INTERNATIONAL COPYRIGHT LAW
 *   AND MAY NOT BE RESOLD OR REDISTRIBUTED. USAGE IS BOUND TO THE PSPDFKIT LICENSE AGREEMENT.
 *   UNAUTHORIZED REPRODUCTION OR DISTRIBUTION IS SUBJECT TO CIVIL AND CRIMINAL PENALTIES.
 *   This notice may not be removed from this file.
 */

package com.pspdfkit.views;

import static com.pspdfkit.configuration.signatures.SignatureSavingStrategy.*;
import static com.pspdfkit.react.helper.ConversionHelpers.getAnnotationTypeFromString;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.Choreographer;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.FragmentManager;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.events.EventDispatcher;
import com.pspdfkit.PSPDFKit;
import com.pspdfkit.annotations.Annotation;
import com.pspdfkit.annotations.AnnotationFlags;
import com.pspdfkit.annotations.AnnotationProvider;
import com.pspdfkit.annotations.AnnotationType;
import com.pspdfkit.annotations.appearance.AssetAppearanceStreamGenerator;
import com.pspdfkit.annotations.configuration.AnnotationConfiguration;
import com.pspdfkit.annotations.configuration.FreeTextAnnotationConfiguration;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.configuration.sharing.ShareFeatures;
import com.pspdfkit.document.ImageDocument;
import com.pspdfkit.document.ImageDocumentLoader;
import com.pspdfkit.document.PdfDocument;
import com.pspdfkit.document.PdfDocumentLoader;
import com.pspdfkit.document.formatters.DocumentJsonFormatter;
import com.pspdfkit.document.formatters.XfdfFormatter;
import com.pspdfkit.document.providers.ContentResolverDataProvider;
import com.pspdfkit.document.providers.DataProvider;
import com.pspdfkit.exceptions.InvalidPasswordException;
import com.pspdfkit.forms.ChoiceFormElement;
import com.pspdfkit.forms.ComboBoxFormElement;
import com.pspdfkit.forms.EditableButtonFormElement;
import com.pspdfkit.forms.FormField;
import com.pspdfkit.forms.TextFormElement;
import com.pspdfkit.internal.model.ImageDocumentImpl;
import com.pspdfkit.listeners.OnVisibilityChangedListener;
import com.pspdfkit.listeners.SimpleDocumentListener;
import com.pspdfkit.react.PDFDocumentModule;
import com.pspdfkit.react.R;
import com.pspdfkit.react.events.CustomAnnotationContextualMenuItemTappedEvent;
import com.pspdfkit.react.events.PdfViewAnnotationChangedEvent;
import com.pspdfkit.react.events.PdfViewTappedEvent;
import com.pspdfkit.react.ConfigurationAdapter;
import com.pspdfkit.react.events.PdfViewAnnotationTappedEvent;
import com.pspdfkit.react.events.PdfViewDataReturnedEvent;
import com.pspdfkit.react.events.PdfViewDocumentLoadFailedEvent;
import com.pspdfkit.react.events.PdfViewDocumentLoadedEvent;
import com.pspdfkit.react.events.PdfViewDocumentSaveFailedEvent;
import com.pspdfkit.react.events.PdfViewDocumentSavedEvent;
import com.pspdfkit.react.events.PdfViewNavigationButtonClickedEvent;
import com.pspdfkit.react.events.CustomToolbarButtonTappedEvent;
import com.pspdfkit.react.events.PdfViewStateChangedEvent;
import com.pspdfkit.react.events.PdfViewZoomLevelChangedEvent;
import com.pspdfkit.react.helper.ConversionHelpers;
import com.pspdfkit.react.helper.DocumentJsonDataProvider;
import com.pspdfkit.react.helper.MeasurementsHelper;
import com.pspdfkit.react.helper.RemoteDocumentDownloader;
import com.pspdfkit.react.menu.ContextualToolbarMenuItemConfig;
import com.pspdfkit.signatures.storage.DatabaseSignatureStorage;
import com.pspdfkit.signatures.storage.SignatureStorage;
import com.pspdfkit.react.helper.PSPDFKitUtils;
import com.pspdfkit.ui.DocumentDescriptor;
import com.pspdfkit.ui.PdfFragment;
import com.pspdfkit.ui.PdfUiFragment;
import com.pspdfkit.ui.PdfUiFragmentBuilder;
import com.pspdfkit.ui.fonts.Font;
import com.pspdfkit.ui.fonts.FontManager;
import com.pspdfkit.ui.search.PdfSearchView;
import com.pspdfkit.ui.search.PdfSearchViewInline;
import com.pspdfkit.ui.special_mode.controller.AnnotationTool;
import com.pspdfkit.ui.toolbar.ContextualToolbarMenuItem;
import com.pspdfkit.ui.toolbar.grouping.MenuItemGroupingRule;
import com.pspdfkit.utils.Size;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import kotlin.Unit;

/**
 * This view displays a {@link com.pspdfkit.ui.PdfFragment} and all associated toolbars.
 */
@SuppressLint("pspdfkit-experimental")
public class PdfView extends FrameLayout {

    private static final String FILE_SCHEME = "file:///";

    /** Key to use when setting the id argument of PdfFragments created by this PdfView. */
    private static final String ARG_ROOT_ID = "root_id";
    private static final String TAG = "PdfView";

    private FragmentManager fragmentManager;
    private EventDispatcher eventDispatcher;
    private String fragmentTag;
    private PdfActivityConfiguration configuration;
    private Disposable documentOpeningDisposable;
    private PdfDocument document;
    private String documentPath;
    private String documentPassword;
    private ReadableMap remoteDocumentConfiguration;
    private int pageIndex = 0;
    private PdfActivityConfiguration initialConfiguration;
    private ReadableArray pendingToolbarItems;

    private boolean isActive = true;

    private PdfViewModeController pdfViewModeController;
    private PdfViewDocumentListener pdfViewDocumentListener;
    private MenuItemListener menuItemListener;
    private ToolbarMenuItemListener toolbarMenuItemListener;

    @NonNull
    private CompositeDisposable pendingFragmentActions = new CompositeDisposable();

    @Nullable
    private PdfUiFragment fragment;

    /** We wrap the fragment in a list so we can have a state that encapsulates no element being set. */
    @NonNull
    private final BehaviorSubject<List<PdfUiFragment>> pdfUiFragmentGetter = BehaviorSubject.createDefault(Collections.emptyList());

    /** An internal id we generate so we can track if fragments found belong to this specific PdfView instance. */
    private int internalId;

    /** We keep track if the navigation button should be shown so we can show it when the inline search view is closed. */
    private boolean isNavigationButtonShown = false;
    /** We keep track if the inline search view is shown since we don't want to add a second navigation button while it is shown. */
    private boolean isSearchViewShown = false;

    /** Indicates whether the image document annotations should be flattened only or flattened and embedded. */
    private String imageSaveMode = "flatten";

    /** Disposable keeping track of our subscription to update the annotation configuration on each emitted PdfFragment. */
    @Nullable
    private Disposable updateAnnotationConfigurationDisposable;

    /** Disposable keeping track of our subscription to update the annotation overlay configuration. */
    @Nullable
    private Disposable setupFragmentDisposable;

    /** The currently configured array of available font names for free text annotations. */
    @Nullable
    private ReadableArray availableFontNames;

    /** The currently configured default font name for free text annotations. */
    @Nullable
    private String selectedFontName;

    /** For annotations with a custom appearance stream, store it. */
    private final Map<String, AssetAppearanceStreamGenerator> customAppearanceStreamGenerators = new HashMap<>();

    @Nullable
    private Map<AnnotationType, AnnotationConfiguration> annotationsConfigurations;

    @Nullable
    private ReadableArray measurementValueConfigurations;

    public PdfView(@NonNull Context context) {
        super(context);
        init();
    }

    public PdfView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PdfView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public PdfView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        pdfViewModeController = new PdfViewModeController(this);

        Choreographer.getInstance().postFrameCallback(new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                manuallyLayoutChildren();
                getViewTreeObserver().dispatchOnGlobalLayout();
                if (isActive) {
                    Choreographer.getInstance().postFrameCallback(this);
                }
            }
        });

        // Set a default configuration.
        configuration = new PdfActivityConfiguration.Builder(getContext()).build();

        // Generate an id to set on all fragments created by the PdfView.
        internalId = View.generateViewId();
    }

    public void inject(FragmentManager fragmentManager, EventDispatcher eventDispatcher) {
        this.fragmentManager = fragmentManager;
        this.eventDispatcher = eventDispatcher;
        pdfViewDocumentListener = new PdfViewDocumentListener(this,
            eventDispatcher);
        menuItemListener = new MenuItemListener(this, eventDispatcher, getContext());
        toolbarMenuItemListener = new ToolbarMenuItemListener(this, eventDispatcher, getContext());
    }

    public void setFragmentTag(String fragmentTag) {
        this.fragmentTag = fragmentTag;
        setupFragment(false);
    }

    public void setInitialConfiguration(PdfActivityConfiguration configuration) {
        this.initialConfiguration = configuration;
    }

    public PdfActivityConfiguration getInitialConfiguration() {
        return this.initialConfiguration;
    }

    public void setPendingToolbarItems(ReadableArray toolbarItems) {
        this.pendingToolbarItems = toolbarItems;
    }

    public ReadableArray getPendingToolbarItems() {
        return this.pendingToolbarItems;
    }

    public void setConfiguration(PdfActivityConfiguration configuration) {
        if (configuration != null && !configuration.equals(this.configuration)) {
            // The configuration changed, recreate the fragment.
            // We set the current page index so the fragment is created at this location.
            this.pageIndex = fragment != null ? fragment.getPageIndex() : this.pageIndex;
            removeFragment(false);
        }
        this.configuration = configuration;
        setupFragment(false);
    }

    public PdfActivityConfiguration getConfiguration() {
        return configuration;
    }

    public void setCustomToolbarItems(final ArrayList toolbarItems) {
        pendingFragmentActions.add(getCurrentPdfUiFragment()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pdfUiFragment -> {
                        ((ReactPdfUiFragment) pdfUiFragment).setCustomToolbarItems(toolbarItems, menuItemListener);

        }));
    }

    public void setAnnotationConfiguration(final  Map<AnnotationType,AnnotationConfiguration> annotationsConfigurations) {
        this.annotationsConfigurations = annotationsConfigurations;
        setupFragment(false);
    }

    public void setDocumentPassword(@Nullable String documentPassword) {
        this.documentPassword = documentPassword;
    }

    public void setRemoteDocumentConfiguration(@Nullable ReadableMap remoteDocumentConfig) {
        this.remoteDocumentConfiguration = remoteDocumentConfig;
    }

    public void setDocument(@Nullable String documentPath, ReactApplicationContext reactApplicationContext) {
        if (documentPath == null) {
            this.document = null;
            removeFragment(false);
            return;
        }

        if (Uri.parse(documentPath).getScheme() == null) {
            // If there is no scheme it might be a raw path.
            try {
                File file = new File(documentPath);
                documentPath = Uri.fromFile(file).toString();
            } catch (Exception e) {
                documentPath = FILE_SCHEME + document;
            }
        }
        if (documentOpeningDisposable != null) {
            documentOpeningDisposable.dispose();
        }
        this.documentPath = documentPath;
        updateState();

        if (Uri.parse(documentPath).getScheme().toLowerCase(Locale.getDefault()).contains("http")) {
            String outputFilePath = this.remoteDocumentConfiguration != null &&
                    this.remoteDocumentConfiguration.hasKey("outputFilePath") ?
                    this.remoteDocumentConfiguration.getString("outputFilePath") : null;

            // If no output file was specified, the temporary file location should always be overwritten
            Boolean overwriteExisting = this.remoteDocumentConfiguration != null &&
                    this.remoteDocumentConfiguration.hasKey("overwriteExisting") ?
                    this.remoteDocumentConfiguration.getBoolean("overwriteExisting") : (outputFilePath == null ? true : false);

            RemoteDocumentDownloader downloader = new RemoteDocumentDownloader(documentPath, outputFilePath, overwriteExisting, getContext(), fragmentManager);
            downloader.startDownload(fileLocation -> {
                if (fileLocation != null) {
                    documentOpeningDisposable = PdfDocumentLoader.openDocumentAsync(getContext(), Uri.fromFile(fileLocation), documentPassword)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(pdfDocument -> {
                                PdfView.this.document = pdfDocument;
                                reactApplicationContext.getNativeModule(PDFDocumentModule.class).setDocument(pdfDocument, this.getId());
                                setupFragment(false);
                            }, throwable -> {
                                // The Android SDK will present password UI, do not emit an error.
                                if (!(throwable instanceof InvalidPasswordException)) {
                                    PdfView.this.document = null;
                                    eventDispatcher.dispatchEvent(new PdfViewDocumentLoadFailedEvent(getId(), throwable.getMessage()));
                                }
                                setupFragment(true);
                            });
                }
                return Unit.INSTANCE;
            });
        } else {
            if (PSPDFKitUtils.isValidImage(documentPath)) {
                documentOpeningDisposable = ImageDocumentLoader.openDocumentAsync(getContext(), Uri.parse(documentPath))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(imageDocument -> {
                            PdfView.this.document = imageDocument.getDocument();
                            reactApplicationContext.getNativeModule(PDFDocumentModule.class).setDocument(imageDocument.getDocument(), this.getId());
                            setupFragment(false);
                        }, throwable -> {
                            PdfView.this.document = null;
                            setupFragment(false);
                            eventDispatcher.dispatchEvent(new PdfViewDocumentLoadFailedEvent(getId(), throwable.getMessage()));
                        });
            } else {
                documentOpeningDisposable = PdfDocumentLoader.openDocumentAsync(getContext(), Uri.parse(documentPath), documentPassword)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(pdfDocument -> {
                            PdfView.this.document = pdfDocument;
                            reactApplicationContext.getNativeModule(PDFDocumentModule.class).setDocument(pdfDocument, this.getId());
                            setupFragment(false);
                        }, throwable -> {
                            // The Android SDK will present password UI, do not emit an error.
                            if (!(throwable instanceof InvalidPasswordException)) {
                                PdfView.this.document = null;
                                eventDispatcher.dispatchEvent(new PdfViewDocumentLoadFailedEvent(getId(), throwable.getMessage()));
                            }
                            setupFragment(true);
                        });
                }
        }
    }

    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
        setupFragment(false);
    }

    public void setDisableDefaultActionForTappedAnnotations(boolean disableDefaultActionForTappedAnnotations) {
        pdfViewDocumentListener.setDisableDefaultActionForTappedAnnotations(disableDefaultActionForTappedAnnotations);
    }

    public void setDisableAutomaticSaving(boolean disableAutomaticSaving) {
        pdfViewDocumentListener.setDisableAutomaticSaving(disableAutomaticSaving);
    }

    /**
     * Sets the menu item grouping rule that will be used for the annotation creation toolbar.
     */
    public void setMenuItemGroupingRule(@NonNull MenuItemGroupingRule groupingRule) {
        pdfViewModeController.setMenuItemGroupingRule(groupingRule);
    }

    public void setAvailableFontNames(@Nullable final ReadableArray availableFontNames) {
        this.availableFontNames = availableFontNames;
        updateAnnotationConfiguration();
    }

    public void setSelectedFontName(@Nullable final String selectedFontName) {
        this.selectedFontName = selectedFontName;
        updateAnnotationConfiguration();
    }

    private void updateAnnotationConfiguration() {
        if (updateAnnotationConfigurationDisposable != null) {
            updateAnnotationConfigurationDisposable.dispose();
        }

        // First we create the new FreeTextAnnotationConfiguration.
        FreeTextAnnotationConfiguration.Builder builder = FreeTextAnnotationConfiguration.builder(getContext());
        FontManager systemFontManager = PSPDFKit.getSystemFontManager();
        if (availableFontNames != null) {
            // Custom list of available fonts is set.
            final ArrayList<Font> availableFonts  = new ArrayList<>();
            for (int i = 0; i < availableFontNames.size(); i++) {
                final String fontName = availableFontNames.getString(i);
                final Font font = systemFontManager.getFontByName(fontName);
                if (font != null) {
                    availableFonts.add(font);
                } else {
                    Log.w(TAG, String.format("Failed to add font %s to list of available fonts since it wasn't found in the list of system fonts.", fontName));
                }
            }
            builder.setAvailableFonts(availableFonts);
        }

        if (selectedFontName != null) {
            final Font defaultFont = systemFontManager.getFontByName(selectedFontName);
            if (defaultFont != null) {
                builder.setDefaultFont(defaultFont);
            } else {
                Log.w(TAG, String.format("Failed to set default font to %s since it wasn't found in the list of system fonts.", selectedFontName));
            }
        }

        final FreeTextAnnotationConfiguration configuration = builder.build();
        // We want to set this on the current PdfFragment and all future ones.
        // We use the observable emitting PdfFragments for this purpose.
        updateAnnotationConfigurationDisposable = getPdfFragment()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(pdfFragment -> {
                if (pdfFragment.getView() != null) {
                    pdfFragment.getAnnotationConfiguration().put(
                            AnnotationTool.FREETEXT, configuration);
                    pdfFragment.getAnnotationConfiguration().put(
                            AnnotationType.FREETEXT, configuration);
                    pdfFragment.getAnnotationConfiguration().put(
                            AnnotationTool.FREETEXT_CALLOUT, configuration);
                }
            });
    }

    public void setShowNavigationButtonInToolbar(final boolean showNavigationButtonInToolbar) {
        isNavigationButtonShown = showNavigationButtonInToolbar;
        pendingFragmentActions.add(getCurrentPdfUiFragment()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(pdfUiFragment -> {
                if (!isSearchViewShown) {
                    ((ReactPdfUiFragment) pdfUiFragment).setShowNavigationButtonInToolbar(showNavigationButtonInToolbar);
                }
            }));
    }

    public void setImageSaveMode(final String imageSaveMode) {
        this.imageSaveMode = imageSaveMode;
    }

    public void setHideDefaultToolbar(boolean hideDefaultToolbar) {
        pendingFragmentActions.add(getCurrentPdfUiFragment()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(pdfUiFragment -> {
                final View views = pdfUiFragment.getView();
                if (views != null) {
                    final ReactMainToolbar mainToolbar = views.findViewById(R.id.pspdf__toolbar_main);
                    if (hideDefaultToolbar) {
                        // If hiding the toolbar is requested we force the visibility to gone, this way it will never be shown.
                        mainToolbar.setForcedVisibility(GONE);
                    } else {
                        // To reset we undo our forcing, and if the UI is supposed to be shown right
                        // now we manually set the visibility to visible so it's immediately shown.
                        mainToolbar.setForcedVisibility(null);
                        if (pdfUiFragment.isUserInterfaceVisible()) {
                            mainToolbar.setVisibility(VISIBLE);
                        }
                    }
                }
            }));
    }

    private void setupFragment(boolean recreate) {
        if (setupFragmentDisposable != null) {
            setupFragmentDisposable.dispose();
            setupFragmentDisposable = null;
        }

        if (fragmentTag != null && configuration != null && (document != null || recreate == true)) {
            PdfUiFragment pdfFragment = (PdfUiFragment) fragmentManager.findFragmentByTag(fragmentTag);
            if (pdfFragment != null &&
                (pdfFragment.getArguments() == null ||
                    pdfFragment.getArguments().getInt(ARG_ROOT_ID) != internalId)) {
                // This is an orphaned fragment probably from a reload, get rid of it.
                fragmentManager.beginTransaction()
                    .remove(pdfFragment)
                    .commitNow();
                pdfFragment = null;
            }

            if (pdfFragment == null) {
                if (recreate == true) {
                    pdfFragment = PdfUiFragmentBuilder.fromUri(getContext(), Uri.parse(this.documentPath)).fragmentClass(ReactPdfUiFragment.class).build();
                } else if (document != null) {
                    pdfFragment = PdfUiFragmentBuilder.fromDocumentDescriptor(getContext(), DocumentDescriptor.fromDocument(document))
                        .configuration(configuration)
                        .fragmentClass(ReactPdfUiFragment.class)
                        .build();
                } else {
                    return;
                }
                // We put our internal id so we can track if this fragment belongs to us, used to handle orphaned fragments after hot reloads.
                pdfFragment.getArguments().putInt(ARG_ROOT_ID, internalId);
                prepareFragment(pdfFragment, true);
            } else {
                View fragmentView = pdfFragment.getView();
                if (pdfFragment.getDocument() != null && !pdfFragment.getDocument().getUid().equals(document.getUid())) {
                    fragmentManager.beginTransaction()
                        .remove(pdfFragment)
                        .commitNow();
                    // The document changed, create a new PdfFragment.
                    pdfFragment = PdfUiFragmentBuilder.fromDocumentDescriptor(getContext(), DocumentDescriptor.fromDocument(document))
                        .configuration(configuration)
                        .fragmentClass(ReactPdfUiFragment.class)
                        .build();
                    prepareFragment(pdfFragment, true);
                } else if (fragmentView != null && fragmentView.getParent() != this) {
                    // We only need to detach the fragment if the parent view changed.
                    fragmentManager.beginTransaction()
                        .remove(pdfFragment)
                        .commitNow();
                    prepareFragment(pdfFragment, true);
                }
            }

            if (pdfFragment.getDocument() != null) {
                if (pageIndex <= document.getPageCount()-1) {
                    pdfFragment.setPageIndex(pageIndex, true);
                }
            }

            fragment = pdfFragment;
            pdfUiFragmentGetter.onNext(Collections.singletonList(pdfFragment));
        }

        /*
        in order to use the NOZOOM feature, we need these lines
        however, we get many crashes during zooming and re-setting markers when we enter overlay mode for stamp annotations
         */
        // put stamp annotations in overlay mode to support NO_ZOOM
//        setupFragmentDisposable = getCurrentPdfFragment().forEach(fragment -> {
//            fragment.setOverlaidAnnotationTypes(EnumSet.of(AnnotationType.STAMP));
//        });
    }

    private void prepareFragment(final PdfUiFragment pdfUiFragment, final boolean attachFragment) {
        if (attachFragment) {

            getRootView().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    // Reattach the fragment if running on API >= 34, there is a compatibility issue with React Native StackScreen fragments.
                    if (Build.VERSION.SDK_INT >= 34) {
                        fragmentManager.beginTransaction()
                                .detach(pdfUiFragment)
                                .commitNow();

                        fragmentManager.beginTransaction()
                                .attach(pdfUiFragment)
                                .commitNow();

                        addView(pdfUiFragment.getView(), LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                        attachPdfFragmentListeners(pdfUiFragment);
                        updateAnnotationConfiguration();
                    }
                    getRootView().getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });

            fragmentManager.beginTransaction()
                .add(pdfUiFragment, fragmentTag)
                .commitNow();

            View fragmentView = pdfUiFragment.getView();
            addView(fragmentView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        }
        attachPdfFragmentListeners(pdfUiFragment);
    }

    private void attachPdfFragmentListeners(final PdfUiFragment pdfUiFragment) {
        pdfUiFragment.setOnContextualToolbarLifecycleListener(pdfViewModeController);
        pdfUiFragment.getPSPDFKitViews().getFormEditingBarView().addOnFormEditingBarLifecycleListener(pdfViewModeController);
        ((ReactPdfUiFragment) pdfUiFragment).setReactPdfUiFragmentListener(new ReactPdfUiFragment.ReactPdfUiFragmentListener() {
            @Override
            public void onConfigurationChanged(@NonNull PdfUiFragment pdfUiFragment) {
                // If the configuration was changed from the UI a new fragment will be created, reattach our listeners.
                prepareFragment(pdfUiFragment, false);
                // Also notify other places that might want to reattach their listeners.
                pdfUiFragmentGetter.onNext(Collections.singletonList(pdfUiFragment));
            }

            @Override
            public void onNavigationButtonClicked(@NonNull PdfUiFragment pdfUiFragment) {
                eventDispatcher.dispatchEvent(new PdfViewNavigationButtonClickedEvent(getId()));
            }
        });

        PdfSearchView searchView = pdfUiFragment.getPSPDFKitViews().getSearchView();
        if (searchView instanceof PdfSearchViewInline) {
            // The inline search view provides its own back button hide ours if it becomes visible.
            searchView.addOnVisibilityChangedListener(new OnVisibilityChangedListener() {
                @Override
                public void onShow(@NonNull View view) {
                    ((ReactPdfUiFragment) pdfUiFragment).setShowNavigationButtonInToolbar(false);
                }

                @Override
                public void onHide(@NonNull View view) {
                    ((ReactPdfUiFragment) pdfUiFragment).setShowNavigationButtonInToolbar(isNavigationButtonShown);
                }
            });
        }

        // After attaching the PdfUiFragment we can access the PdfFragment.
        preparePdfFragment(pdfUiFragment.getPdfFragment());
    }

    private void preparePdfFragment(@NonNull PdfFragment pdfFragment) {
        pdfFragment.addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void onDocumentLoaded(@NonNull PdfDocument document) {
                manuallyLayoutChildren();
                if (pageIndex <= document.getPageCount()-1) {
                    pdfFragment.setPageIndex(pageIndex, false);
                }
                updateState();
            }
        });

        pdfFragment.addOnTextSelectionModeChangeListener(pdfViewModeController);
        pdfFragment.addDocumentListener(pdfViewDocumentListener);
        pdfFragment.addOnAnnotationSelectedListener(pdfViewDocumentListener);
        pdfFragment.addOnAnnotationUpdatedListener(pdfViewDocumentListener);
        if (pdfFragment.getDocument() != null) {
            pdfFragment.getDocument().getFormProvider().addOnFormFieldUpdatedListener(pdfViewDocumentListener);
        }

        // Add annotation configurations.
        if (annotationsConfigurations != null) {
            for (AnnotationType annotationType : annotationsConfigurations.keySet()) {
                AnnotationConfiguration annotationConfiguration = annotationsConfigurations.get(annotationType);
                if (annotationConfiguration != null) {
                    pdfFragment.getAnnotationConfiguration().put(annotationType, annotationConfiguration);
                }
            }
        }

        // Add Measurement configuration
        if (this.measurementValueConfigurations != null) {
            this.applyMeasurementValueConfigurations(pdfFragment, this.measurementValueConfigurations);
        }
          
        // Setup SignatureDatabase if SignatureSaving is enabled.
        if (pdfFragment.getConfiguration().getSignatureSavingStrategy() == ALWAYS_SAVE ||
                pdfFragment.getConfiguration().getSignatureSavingStrategy() == SAVE_IF_SELECTED) {
            final SignatureStorage storage = DatabaseSignatureStorage.withName(getContext(), "SignatureDatabase");
            pdfFragment.setSignatureStorage(storage);
        }
    }

    public void removeFragment(boolean makeInactive) {
        PdfUiFragment pdfUiFragment = (PdfUiFragment) fragmentManager.findFragmentByTag(fragmentTag);
        if (pdfUiFragment != null) {
            fragmentManager.beginTransaction()
                .remove(pdfUiFragment)
                .commitNowAllowingStateLoss();
        }
        if (makeInactive) {
            // Clear everything.
            isActive = false;
            document = null;
            pendingFragmentActions.dispose();
            pendingFragmentActions = new CompositeDisposable();
        }
        fragment = null;
        pdfUiFragmentGetter.onNext(Collections.emptyList());
    }

    void manuallyLayoutChildren() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
            child.layout(0, 0, child.getMeasuredWidth(), child.getMeasuredHeight());
        }
    }

    void updateState() {
        if (fragment != null) {
            updateState(fragment.getPageIndex());
        } else {
            updateState(-1);
        }
    }

    void updateState(int pageIndex) {
        if (fragment != null) {
            if (fragment.getDocument() != null) {
                eventDispatcher.dispatchEvent(new PdfViewStateChangedEvent(
                    getId(),
                    pageIndex,
                    fragment.getDocument().getPageCount(),
                    pdfViewModeController.isAnnotationCreationActive(),
                    pdfViewModeController.isAnnotationEditingActive(),
                    pdfViewModeController.isTextSelectionActive(),
                    pdfViewModeController.isFormEditingActive()));
            } else {
                eventDispatcher.dispatchEvent(new PdfViewStateChangedEvent(getId()));
            }
        }
    }

    void updateZoomLevelEvent(float zoomLevel) {
        eventDispatcher.dispatchEvent(new PdfViewZoomLevelChangedEvent(
                getId(),
                zoomLevel));
    }

    public EventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    public void enterAnnotationCreationMode() {
        pendingFragmentActions.add(getCurrentPdfFragment()
            .observeOn(Schedulers.io())
            .subscribe(PdfFragment::enterAnnotationCreationMode));
    }

    public void exitCurrentlyActiveMode() {
        pendingFragmentActions.add(getCurrentPdfFragment()
            .observeOn(Schedulers.io())
            .subscribe(PdfFragment::exitCurrentlyActiveMode));
    }

    public boolean saveCurrentDocument() throws Exception {
        if (fragment != null) {
            try {
                if (document instanceof ImageDocumentImpl.ImagePdfDocumentWrapper) {
                    boolean metadata = this.imageSaveMode.equals("flattenAndEmbed") ? true : false;
                    if (((ImageDocumentImpl.ImagePdfDocumentWrapper) document).getImageDocument().saveIfModified(metadata)) {
                        // Since the document listeners won't be called when manually saving we also dispatch this event here.
                        eventDispatcher.dispatchEvent(new PdfViewDocumentSavedEvent(getId()));
                        return true;
                    }
                }
                else {
                    if (document.saveIfModified()) {
                        // Since the document listeners won't be called when manually saving we also dispatch this event here.
                        eventDispatcher.dispatchEvent(new PdfViewDocumentSavedEvent(getId()));
                        return true;
                    }
                }
                return false;
            } catch (Exception e) {
                eventDispatcher.dispatchEvent(new PdfViewDocumentSaveFailedEvent(getId(), e.getMessage()));
                throw e;
            }
        }
        return false;
    }

    public Single<List<Annotation>> getAnnotations(final int pageIndex, @Nullable final String type) {
        PdfDocument document = fragment.getDocument();
        if (pageIndex > document.getPageCount()-1) {
            return Single.just(new ArrayList<>());
        }
        return getCurrentPdfFragment()
            .map(PdfFragment::getDocument)
            .flatMap((Function<PdfDocument, ObservableSource<Annotation>>) pdfDocument ->
                pdfDocument.getAnnotationProvider().getAllAnnotationsOfTypeAsync(getAnnotationTypeFromString(type), pageIndex, 1)).toList();
    }

    public Single<List<Annotation>> getAllAnnotations(@Nullable final String type) {
        return getCurrentPdfFragment().map(PdfFragment::getDocument)
            .flatMap(pdfDocument -> pdfDocument.getAnnotationProvider().getAllAnnotationsOfTypeAsync(getAnnotationTypeFromString(type)))
            .toList();
    }

    public Disposable addAnnotation(final int requestId, ReadableMap annotation) {
        return getCurrentPdfFragment().map(PdfFragment::getDocument).subscribeOn(Schedulers.io())
            .map(pdfDocument -> {
                JSONObject json = new JSONObject(annotation.toHashMap());
                Annotation annotationFromInstantJson = pdfDocument.getAnnotationProvider().createAnnotationFromInstantJson(json.toString());
                // vectorStampAssets get their own appearance stream (i.e. look)
                String vectorStampAssetFile = json.optString("vectorStampAsset", null);
                if (vectorStampAssetFile != null) {
                    AssetAppearanceStreamGenerator assetAppearanceStreamGenerator = customAppearanceStreamGenerators.get(vectorStampAssetFile);
                    if (assetAppearanceStreamGenerator == null) {
                        assetAppearanceStreamGenerator = new AssetAppearanceStreamGenerator(vectorStampAssetFile);
                        customAppearanceStreamGenerators.put(vectorStampAssetFile, assetAppearanceStreamGenerator);
                    }
                    annotationFromInstantJson.setAppearanceStreamGenerator(assetAppearanceStreamGenerator);
                }
                return annotationFromInstantJson;
            })
            .map(Annotation::toInstantJson)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe((instantJson) -> eventDispatcher.dispatchEvent(new PdfViewDataReturnedEvent(getId(), requestId, true)),
                (throwable) -> eventDispatcher.dispatchEvent(new PdfViewDataReturnedEvent(getId(), requestId, throwable)));
    }

    public Disposable removeAnnotation(final int requestId, ReadableMap annotation) {
        return getCurrentPdfFragment().map(PdfFragment::getDocument).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap(pdfDocument -> {
                JSONObject json = new JSONObject(annotation.toHashMap());
                // We can't create an annotation from the instant json since that will attach it to the document,
                // so we manually grab the necessary values.
                int pageIndex = json.optInt("pageIndex", -1);
                String type = json.optString("type", null);
                String name = json.optString("name", null);
                if (pageIndex == -1 || type == null || name == null) {
                    return Observable.empty();
                }

                return pdfDocument.getAnnotationProvider().getAllAnnotationsOfTypeAsync(getAnnotationTypeFromString(type), pageIndex, 1)
                    .filter(annotationToFilter -> name.equals(annotationToFilter.getName()))
                    .map(filteredAnnotation -> new Pair<>(filteredAnnotation, pdfDocument));
            })
            .firstOrError()
            .flatMapCompletable(pair -> Completable.fromAction(() -> {
                pair.second.getAnnotationProvider().removeAnnotationFromPage(pair.first);
            }))
            .subscribe(() -> eventDispatcher.dispatchEvent(new PdfViewDataReturnedEvent(getId(), requestId, true)), (throwable -> {
                if (throwable instanceof NoSuchElementException) {
                    // We didn't find an annotation so return false.
                    eventDispatcher.dispatchEvent(new PdfViewDataReturnedEvent(getId(), requestId, false));
                } else {
                    eventDispatcher.dispatchEvent(new PdfViewDataReturnedEvent(getId(), requestId, throwable));
                }
            }));
    }

    public Single<JSONObject> getAllUnsavedAnnotations() {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        return DocumentJsonFormatter.exportDocumentJsonAsync(document, outputStream)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .toSingle(() -> {
                    try {
                        return new JSONObject(outputStream.toString());
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public Disposable addAnnotations(final int requestId, ReadableMap annotation) {
        return getCurrentPdfFragment().map(PdfFragment::getDocument).subscribeOn(Schedulers.io())
            .flatMapCompletable(currentDocument -> Completable.fromAction(() -> {
                JSONObject json = new JSONObject(annotation.toHashMap());
                final DataProvider dataProvider = new DocumentJsonDataProvider(json);
                DocumentJsonFormatter.importDocumentJson(currentDocument, dataProvider);
            }))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(() -> eventDispatcher.dispatchEvent(new PdfViewDataReturnedEvent(getId(), requestId, true)),
                (throwable) -> eventDispatcher.dispatchEvent(new PdfViewDataReturnedEvent(getId(), requestId, throwable)));
    }

    public Disposable setAnnotationFlags(final int requestId, String uuid, ReadableArray flags) {
        AtomicBoolean found = new AtomicBoolean(false);
        return getCurrentPdfFragment().map(PdfFragment::getDocument).subscribeOn(Schedulers.io())
                .flatMapCompletable(currentDocument -> Completable.fromAction(() -> {
                    List<Annotation> allAnnotations = currentDocument.getAnnotationProvider().getAllAnnotationsOfType(AnnotationProvider.ALL_ANNOTATION_TYPES);
                    for (int i = 0; i < allAnnotations.size(); i++) {
                        Annotation annotation = allAnnotations.get(i);
                        if (annotation.getUuid().equals(uuid) || 
                           (annotation.getName() != null && annotation.getName().equals(uuid))) {
                            EnumSet<AnnotationFlags> convertedFlags = ConversionHelpers.getAnnotationFlags(flags);
                            annotation.setFlags(convertedFlags);
                            getCurrentPdfFragment().subscribe(pdfFragment -> {
                               pdfFragment.notifyAnnotationHasChanged(annotation);
                            });
                            found.set(true);
                            break;
                        }
                    }
                }))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> eventDispatcher.dispatchEvent(new PdfViewDataReturnedEvent(getId(), requestId, found.get())),
                        (throwable) -> eventDispatcher.dispatchEvent(new PdfViewDataReturnedEvent(getId(), requestId, throwable)));
    }

    public Disposable getAnnotationFlags(final int requestId, @NonNull String uuid) {
        return getCurrentPdfFragment().map(PdfFragment::getDocument).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(currentDocument -> {
                    List<Annotation> allAnnotations = currentDocument.getAnnotationProvider().getAllAnnotationsOfType(AnnotationProvider.ALL_ANNOTATION_TYPES);
                    ArrayList<String> convertedFlags = new ArrayList<>();
                    for (int i = 0; i < allAnnotations.size(); i++) {
                        Annotation annotation = allAnnotations.get(i);
                        if (annotation.getUuid().equals(uuid) || 
                           (annotation.getName() != null && annotation.getName().equals(uuid))) {
                            EnumSet<AnnotationFlags> flags = annotation.getFlags();
                            convertedFlags = ConversionHelpers.convertAnnotationFlags(flags);
                            break;
                        }
                    }
                    eventDispatcher.dispatchEvent(new PdfViewDataReturnedEvent(getId(), requestId, convertedFlags));
                });
    }

    public Disposable getFormFieldValue(final int requestId, @NonNull String formElementName) {
        return document.getFormProvider().getFormElementWithNameAsync(formElementName)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(formElement -> {
                JSONObject result = new JSONObject();
                if (formElement instanceof TextFormElement) {
                    TextFormElement textFormElement = (TextFormElement) formElement;
                    String text = textFormElement.getText();
                    if (text == null || text.isEmpty()) {
                        result.put("value", JSONObject.NULL);
                    } else {
                        result.put("value", text);
                    }
                } else if (formElement instanceof EditableButtonFormElement) {
                    EditableButtonFormElement editableButtonFormElement = (EditableButtonFormElement) formElement;
                    if (editableButtonFormElement.isSelected()) {
                        result.put("value", "selected");
                    } else {
                        result.put("value", "deselected");
                    }
                } else if (formElement instanceof ComboBoxFormElement) {
                    ComboBoxFormElement comboBoxFormElement = (ComboBoxFormElement) formElement;
                    if (comboBoxFormElement.isCustomTextSet()) {
                        result.put("value", comboBoxFormElement.getCustomText());
                    } else {
                        result.put("value", comboBoxFormElement.getSelectedIndexes());
                    }
                } else if (formElement instanceof ChoiceFormElement) {
                    result.put("value", ((ChoiceFormElement) formElement).getSelectedIndexes());
                }

                if (result.length() == 0) {
                    // No type was applicable.
                    result.put("error", "Unsupported form field encountered");
                    eventDispatcher.dispatchEvent(new PdfViewDataReturnedEvent(getId(), requestId, result));
                } else {
                    eventDispatcher.dispatchEvent(new PdfViewDataReturnedEvent(getId(), requestId, result));
                }
            },
                throwable -> eventDispatcher.dispatchEvent(new PdfViewDataReturnedEvent(getId(), requestId, throwable)),
                () -> {
                    try {
                        JSONObject result = new JSONObject();
                        result.put("error", "Failed to get the form field value.");
                        eventDispatcher.dispatchEvent(new PdfViewDataReturnedEvent(getId(), requestId, result));
                    } catch (Exception e) {
                        eventDispatcher.dispatchEvent(new PdfViewDataReturnedEvent(getId(), requestId, e));
                    }
                });

    }

    public Maybe<Boolean> setFormFieldValue(@NonNull String formElementName, @NonNull final String value) {
        return document.getFormProvider().getFormElementWithNameAsync(formElementName)
            .map(formElement -> {
                if (formElement instanceof TextFormElement) {
                    TextFormElement textFormElement = (TextFormElement) formElement;
                    textFormElement.setText(value);
                    return true;
                } else if (formElement instanceof EditableButtonFormElement) {
                    EditableButtonFormElement editableButtonFormElement = (EditableButtonFormElement) formElement;
                    if (value.equalsIgnoreCase("selected")) {
                        editableButtonFormElement.select();
                    } else if (value.equalsIgnoreCase("deselected")) {
                        editableButtonFormElement.deselect();
                    }
                    return true;
                } else if (formElement instanceof ChoiceFormElement) {
                    ChoiceFormElement choiceFormElement = (ChoiceFormElement) formElement;
                    try {
                        int selectedIndex = Integer.parseInt(value);
                        List<Integer> selectedIndices = new ArrayList<>();
                        selectedIndices.add(selectedIndex);
                        choiceFormElement.setSelectedIndexes(selectedIndices);
                        return true;
                    } catch (NumberFormatException e) {
                        try {
                            // Maybe it's multiple indices.
                            JSONArray indices = new JSONArray(value);
                            List<Integer> selectedIndices = new ArrayList<>();
                            for (int i = 0; i < indices.length(); i++) {
                                selectedIndices.add(indices.getInt(i));
                            }
                            choiceFormElement.setSelectedIndexes(selectedIndices);
                            return true;
                        } catch (JSONException ex) {
                            // This isn't an index maybe we can set a custom value on a combo box.
                            if (formElement instanceof ComboBoxFormElement) {
                                ((ComboBoxFormElement) formElement).setCustomText(value);
                                return true;
                            }
                        }
                    }
                }
                return false;
            });
    }

    public Disposable importXFDF(final int requestId, String filePath) {

        if (Uri.parse(filePath).getScheme() == null) {
            filePath = FILE_SCHEME + filePath;
        }
        if (fragment == null || fragment.getDocument() == null) {
            return null;
        }

        return XfdfFormatter.parseXfdfAsync(fragment.getDocument(), new ContentResolverDataProvider(Uri.parse(filePath)))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(annotations -> {
                    for (Annotation annotation : annotations) {
                        fragment.getDocument().getAnnotationProvider().addAnnotationToPage(annotation);
                    }
                    JSONObject result = new JSONObject();
                    result.put("success", true);
                    eventDispatcher.dispatchEvent(new PdfViewDataReturnedEvent(getId(), requestId, result));
                    }, throwable -> {
                    eventDispatcher.dispatchEvent(new PdfViewDataReturnedEvent(getId(), requestId, throwable));
                });
    }

    public Disposable exportXFDF(final int requestId, String filePath) {

        if (Uri.parse(filePath).getScheme() == null) {
            filePath = FILE_SCHEME + filePath;
        }
        if (fragment == null || fragment.getDocument() == null) {
            return null;
        }

        try {
            final OutputStream outputStream =  getContext().getContentResolver().openOutputStream(Uri.parse(filePath));
            if (outputStream == null) return null;

            List<Annotation> annotations = fragment.getDocument().getAnnotationProvider().getAllAnnotationsOfType(AnnotationProvider.ALL_ANNOTATION_TYPES);
            List<FormField> formFields = fragment.getDocument().getFormProvider().getFormFields();

            String finalFilePath = filePath;
            return XfdfFormatter.writeXfdfAsync(fragment.getDocument(), annotations, formFields, outputStream)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(() -> {
                        JSONObject result = new JSONObject();
                        result.put("success", true);
                        result.put("filePath", finalFilePath);
                                eventDispatcher.dispatchEvent(new PdfViewDataReturnedEvent(getId(), requestId, result));
                            }, throwable -> {
                                eventDispatcher.dispatchEvent(new PdfViewDataReturnedEvent(getId(), requestId, throwable));
                            }
                    );
        } catch (final FileNotFoundException ignored) {
            eventDispatcher.dispatchEvent(new PdfViewDataReturnedEvent(getId(), requestId, ignored));
        }
        return null;
    }

    public JSONObject convertConfiguration() {
        try {
            JSONObject config = new JSONObject();
            config.put("scrollDirection", ConfigurationAdapter.getStringValueForConfigurationItem(configuration.getConfiguration().getScrollDirection()));
            config.put("pageTransition", ConfigurationAdapter.getStringValueForConfigurationItem(configuration.getConfiguration().getScrollMode()));
            config.put("enableTextSelection", configuration.getConfiguration().isTextSelectionEnabled());
            config.put("autosaveEnabled", configuration.getConfiguration().isAutosaveEnabled());
            config.put("disableAutomaticSaving", !configuration.getConfiguration().isAutosaveEnabled());
            config.put("signatureSavingStrategy", ConfigurationAdapter.getStringValueForConfigurationItem(configuration.getConfiguration().getSignatureSavingStrategy()));

            config.put("pageMode", ConfigurationAdapter.getStringValueForConfigurationItem(configuration.getConfiguration().getLayoutMode()));
            config.put("firstPageAlwaysSingle", configuration.getConfiguration().isFirstPageAlwaysSingle());
            config.put("showPageLabels", configuration.isShowPageLabels());
            config.put("documentLabelEnabled", configuration.isShowDocumentTitleOverlayEnabled());
            config.put("spreadFitting", ConfigurationAdapter.getStringValueForConfigurationItem(configuration.getConfiguration().getFitMode()));
            config.put("invertColors", configuration.getConfiguration().isInvertColors());
            config.put("androidGrayScale", configuration.getConfiguration().isToGrayscale());

            config.put("userInterfaceViewMode", ConfigurationAdapter.getStringValueForConfigurationItem(configuration.getUserInterfaceViewMode()));
            config.put("inlineSearch", configuration.getSearchType() == PdfActivityConfiguration.SEARCH_INLINE ? true : false);
            config.put("immersiveMode", configuration.isImmersiveMode());
            config.put("toolbarTitle", configuration.getActivityTitle());
            config.put("androidShowSearchAction", configuration.isSearchEnabled());
            config.put("androidShowOutlineAction", configuration.isOutlineEnabled());
            config.put("androidShowBookmarksAction", configuration.isBookmarkListEnabled());
            config.put("androidShowShareAction", configuration.getConfiguration().getEnabledShareFeatures() == ShareFeatures.all() ? true : false);
            config.put("androidShowPrintAction", configuration.isPrintingEnabled());
            config.put("androidShowDocumentInfoView", configuration.isDocumentInfoViewEnabled());
            config.put("androidShowSettingsMenu", configuration.isSettingsItemEnabled());

            config.put("showThumbnailBar", ConfigurationAdapter.getStringValueForConfigurationItem(configuration.getThumbnailBarMode()));
            config.put("androidShowThumbnailGridAction", configuration.isThumbnailGridEnabled());

            config.put("editableAnnotationTypes", ConfigurationAdapter.getStringValuesForConfigurationItems(configuration.getConfiguration().getEditableAnnotationTypes()));
            config.put("enableAnnotationEditing", configuration.getConfiguration().isAnnotationEditingEnabled());
            config.put("enableFormEditing", configuration.getConfiguration().isFormEditingEnabled());
            config.put("androidShowAnnotationListAction", configuration.isAnnotationListEnabled());

            return config;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    /** Returns the {@link PdfFragment} hosted in the current {@link PdfUiFragment}. */
    private Observable<PdfFragment> getCurrentPdfFragment() {
        return getPdfFragment()
            .take(1);
    }

    /** Returns the {@link PdfUiFragment}. */
    private Observable<PdfUiFragment> getCurrentPdfUiFragment() {
        return pdfUiFragmentGetter
            .filter(pdfUiFragments -> !pdfUiFragments.isEmpty())
            .map(pdfUiFragments -> pdfUiFragments.get(0))
            .take(1);
    }

    /**
     * Returns the current fragment if it is set. You should not cache a reference to this as it might be replaced.
     * If you want to register listeners on the {@link PdfFragment} you should observe the result of {@link #getPdfFragment()}
     * and setup the listeners in there. This way if the fragment is replaced your listeners will be setup again.
     */
    public Maybe<PdfFragment> getActivePdfFragment() {
        return getCurrentPdfFragment().firstElement();
    }

    /**
     * This returns {@link PdfFragment} as they become available. If the user changes the view configuration or the fragment is replaced for other reasons a new {@link PdfFragment} is emitted.
     */
    public Observable<PdfFragment> getPdfFragment() {
        return pdfUiFragmentGetter
            .filter(pdfUiFragments -> !pdfUiFragments.isEmpty())
            .map(pdfUiFragments -> pdfUiFragments.get(0))
            .filter(pdfUiFragment -> pdfUiFragment.getPdfFragment() != null)
            .map(PdfUiFragment::getPdfFragment);
    }

    /** Returns the event registration map for the default events emitted by the {@link PdfView}. */
    public static  Map<String, Map<String, String>> createDefaultEventRegistrationMap() {
       Map<String , Map<String, String>> map = MapBuilder.of(PdfViewStateChangedEvent.EVENT_NAME, MapBuilder.of("registrationName", "onStateChanged"),
            PdfViewDocumentSavedEvent.EVENT_NAME, MapBuilder.of("registrationName", "onDocumentSaved"),
            PdfViewAnnotationTappedEvent.EVENT_NAME, MapBuilder.of("registrationName", "onAnnotationTapped"),
            PdfViewAnnotationChangedEvent.EVENT_NAME, MapBuilder.of("registrationName", "onAnnotationsChanged"),
            PdfViewDataReturnedEvent.EVENT_NAME, MapBuilder.of("registrationName", "onDataReturned"),
            PdfViewDocumentSaveFailedEvent.EVENT_NAME, MapBuilder.of("registrationName", "onDocumentSaveFailed"),
            PdfViewDocumentLoadFailedEvent.EVENT_NAME, MapBuilder.of("registrationName", "onDocumentLoadFailed")
        );
       map.put(PdfViewNavigationButtonClickedEvent.EVENT_NAME, MapBuilder.of("registrationName", "onNavigationButtonClicked"));
       map.put(PdfViewZoomLevelChangedEvent.EVENT_NAME, MapBuilder.of("registrationName", "onZoomLevelChanged"));
       map.put(PdfViewTappedEvent.EVENT_NAME, MapBuilder.of("registrationName", "onPageTouched"));

       map.put(PdfViewDocumentLoadedEvent.EVENT_NAME, MapBuilder.of("registrationName", "onDocumentLoaded"));
       map.put(CustomToolbarButtonTappedEvent.EVENT_NAME, MapBuilder.of("registrationName", "onCustomToolbarButtonTapped"));
       map.put(CustomAnnotationContextualMenuItemTappedEvent.EVENT_NAME, MapBuilder.of("registrationName", "onCustomAnnotationContextualMenuItemTapped"));
       return map;
    }

    public Maybe<JSONObject> getSizeOfFirstPage() {
        return getCurrentPdfFragment().map(PdfFragment::getDocument).map(document -> {
            Size sizeOfFirstPage = document.getPageSize(0);
            JSONObject result = new JSONObject();
            try {
                result.put("height", sizeOfFirstPage.height);
                result.put("width", sizeOfFirstPage.width);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return result;
        }).elementAt(0);
    }

    private void applyMeasurementValueConfigurations(PdfFragment fragment, ReadableArray measurementConfigs) {
        if (this.measurementValueConfigurations != null) {
            for (int i = 0; i < this.measurementValueConfigurations.size(); i++) {
                ReadableMap configuration = this.measurementValueConfigurations.getMap(i);
                MeasurementsHelper.addMeasurementConfiguration(fragment, configuration.toHashMap());
            }
        }
    }

    /**
     * Sets the MeasurementValuesConfigurations on the current pdfFragment during setup, also saves it if fragment changes occur
     * @param measurementConfigs
     */
    public void setMeasurementValueConfigurations(ReadableArray measurementConfigs) {
        this.measurementValueConfigurations = measurementConfigs;
        if (fragment != null && fragment.getPdfFragment() != null) {
            this.applyMeasurementValueConfigurations(fragment.getPdfFragment(), measurementConfigs);
        }
    }

    /**
     * Returns the current MeasurementValuesConfigurations
     * @return List of MeasurementValueConfiguration objects
     */
    public JSONObject getMeasurementValueConfigurations() throws JSONException {

        JSONObject result = new JSONObject();
        if (fragment != null && fragment.getPdfFragment() != null) {
            List configs = MeasurementsHelper.getMeasurementConfigurations(fragment.getPdfFragment());
            result.put("measurementValueConfigurations", configs);
            return result;
        }
        return result;
    }

    /**
     * Sets the Annotation menu toolbar items on the current pdfFragment during setup
     * @param annotationContextualMenuItems
     */
    public void setAnnotationToolbarMenuButtonItems(ReadableMap annotationContextualMenuItems) {
        List<ContextualToolbarMenuItem> toolbarMenuItems = new ArrayList<>();
        ReadableArray menuItems = annotationContextualMenuItems.getArray("buttons");
        boolean retainSuggestedMenuItems = annotationContextualMenuItems.hasKey("retainSuggestedMenuItems") ? annotationContextualMenuItems.getBoolean("retainSuggestedMenuItems") : true;
        String position = annotationContextualMenuItems.hasKey("position") ? annotationContextualMenuItems.getString("position") : "end";
        ContextualToolbarMenuItem.Position buttonPosition = ConversionHelpers.getContextualToolbarMenuItemPosition(position);

        for (int i = 0; i < menuItems.size(); i++) {
            ReadableMap item = menuItems.getMap(i);
            String customId = item.getString("id");
            String image = item.getString("image");
            String title = item.hasKey("title") ? item.getString("title") : customId;
            boolean selectable = item.hasKey("selectable") ? item.getBoolean("selectable") : false;
            int resId = PSPDFKitUtils.getCustomResourceId(customId, "id", getContext());
            int iconId = PSPDFKitUtils.getCustomResourceId(image, "drawable", getContext());

            // Apply contextual toolbar theme color to custom menu item icon
            Drawable customIcon = ContextCompat.getDrawable(getContext(), iconId);
            final TypedArray a = getContext().getTheme().obtainStyledAttributes(
                    null,
                    com.pspdfkit.R.styleable.pspdf__ContextualToolbar,
                    com.pspdfkit.R.attr.pspdf__contextualToolbarStyle,
                    com.pspdfkit.R.style.PSPDFKit_ContextualToolbar
            );
            int contextualToolbarIconsColor = a.getColor(com.pspdfkit.R.styleable.pspdf__ContextualToolbar_pspdf__iconsColor, ContextCompat.getColor(getContext(), android.R.color.white));
            int contextualToolbarIconsColorActivated = a.getColor(com.pspdfkit.R.styleable.pspdf__ContextualToolbar_pspdf__iconsColorActivated, ContextCompat.getColor(getContext(), android.R.color.white));
            a.recycle();
            try {
                DrawableCompat.setTint(customIcon, contextualToolbarIconsColor);
            } catch (Exception e) {
                // Omit the icon if the image is missing
            }

            ContextualToolbarMenuItem customItem = ContextualToolbarMenuItem.createSingleItem(
                    getContext(),
                    resId,
                    customIcon,
                    title,
                    contextualToolbarIconsColor,
                    contextualToolbarIconsColorActivated,
                    buttonPosition,
                    selectable);

            toolbarMenuItems.add(customItem);
            pendingFragmentActions.add(getCurrentPdfUiFragment()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(pdfUiFragment -> {
                        pdfUiFragment.registerForContextMenu(customItem);
                    }));
        }

        List<Integer> resIds = new ArrayList();
        for (ContextualToolbarMenuItem menuItem : toolbarMenuItems) {
            resIds.add(menuItem.getId());
        }
        toolbarMenuItemListener.setResourceIds(resIds);
        ContextualToolbarMenuItemConfig config = new ContextualToolbarMenuItemConfig(toolbarMenuItems, retainSuggestedMenuItems, toolbarMenuItemListener);
        pdfViewModeController.setAnnotationSelectionMenuConfig(config);
    }
}
