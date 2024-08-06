//
//  Copyright © 2018-2024 PSPDFKit GmbH. All rights reserved.
//
//  THIS SOURCE CODE AND ANY ACCOMPANYING DOCUMENTATION ARE PROTECTED BY INTERNATIONAL COPYRIGHT LAW
//  AND MAY NOT BE RESOLD OR REDISTRIBUTED. USAGE IS BOUND TO THE PSPDFKIT LICENSE AGREEMENT.
//  UNAUTHORIZED REPRODUCTION OR DISTRIBUTION IS SUBJECT TO CIVIL AND CRIMINAL PENALTIES.
//  This notice may not be removed from this file.
//

#import <UIKit/UIKit.h>
#import <React/RCTComponent.h>

@import PSPDFKit;
@import PSPDFKitUI;

NS_ASSUME_NONNULL_BEGIN

@interface RCTPSPDFKitView: UIView

@property (nonatomic, readonly) PSPDFViewController *pdfController;
@property (nonatomic) BOOL hideNavigationBar;
@property (nonatomic, readonly) UIBarButtonItem *closeButton;
@property (nonatomic) BOOL disableDefaultActionForTappedAnnotations;
@property (nonatomic) BOOL disableAutomaticSaving;
@property (nonatomic) PSPDFPageIndex pageIndex;
@property (nonatomic, copy, nullable) NSString *annotationAuthorName;
@property (nonatomic) PSPDFImageSaveMode imageSaveMode;
@property (nonatomic, copy) RCTBubblingEventBlock onCloseButtonPressed;
@property (nonatomic, copy) RCTBubblingEventBlock onDocumentSaved;
@property (nonatomic, copy) RCTBubblingEventBlock onDocumentSaveFailed;
@property (nonatomic, copy) RCTBubblingEventBlock onDocumentLoadFailed;
@property (nonatomic, copy) RCTBubblingEventBlock onAnnotationTapped;
@property (nonatomic, copy) RCTBubblingEventBlock onAnnotationsChanged;
@property (nonatomic, copy) RCTBubblingEventBlock onStateChanged;
@property (nonatomic, copy) RCTBubblingEventBlock onZoomLevelChanged;
@property (nonatomic, copy) RCTBubblingEventBlock onPageTouched;
@property (nonatomic, copy) RCTBubblingEventBlock onDocumentLoaded;
@property (nonatomic, copy) RCTBubblingEventBlock onCustomToolbarButtonTapped;
@property (nonatomic, copy) RCTBubblingEventBlock onCustomAnnotationContextualMenuItemTapped;
@property (nonatomic, copy, nullable) NSArray<NSString *> *availableFontNames;
@property (nonatomic, copy, nullable) NSString *selectedFontName;
@property (nonatomic) BOOL showDownloadableFonts;

@property (nonatomic) UITapGestureRecognizer *tapGesture;

/// Annotation Toolbar
- (BOOL)enterAnnotationCreationMode;
- (BOOL)exitCurrentlyActiveMode;

/// Document
- (BOOL)saveCurrentDocumentWithError:(NSError *_Nullable *)error;

/// Anotations
- (NSDictionary<NSString *, NSArray<NSDictionary *> *> *)getAnnotations:(PSPDFPageIndex)pageIndex type:(PSPDFAnnotationType)type error:(NSError *_Nullable *)error;
- (BOOL)addAnnotation:(id)jsonAnnotation error:(NSError *_Nullable *)error;
- (BOOL)removeAnnotations:(NSArray<NSDictionary *> *)annotationsJSON;
- (NSDictionary<NSString *, NSArray<NSDictionary *> *> *)getAllUnsavedAnnotationsWithError:(NSError *_Nullable *)error;
- (NSDictionary<NSString *, NSArray<NSDictionary *> *> *)getAllAnnotations:(PSPDFAnnotationType)type error:(NSError *_Nullable *)error;
- (BOOL)addAnnotations:(NSString *)jsonAnnotations error:(NSError *_Nullable *)error;
- (BOOL)setAnnotationFlags:(NSString *)uuid flags:(NSArray<NSString *> *)flags;
- (NSArray <NSString *> *)getAnnotationFlags:(NSString *)uuid;

/// Forms
- (NSDictionary<NSString *, NSString *> *)getFormFieldValue:(NSString *)fullyQualifiedName;
- (BOOL)setFormFieldValue:(NSString *)value fullyQualifiedName:(NSString *)fullyQualifiedName;

/// Toolbar buttons customizations
- (void)setLeftBarButtonItems:(nullable NSArray <NSString *> *)items forViewMode:(nullable NSString *) viewMode animated:(BOOL)animated;
- (void)setRightBarButtonItems:(nullable NSArray <NSString *> *)items forViewMode:(nullable NSString *) viewMode animated:(BOOL)animated;
- (NSArray <NSString *> *)getLeftBarButtonItemsForViewMode:(NSString *)viewMode;
- (NSArray <NSString *> *)getRightBarButtonItemsForViewMode:(NSString *)viewMode;

/// XFDF
- (NSDictionary *)importXFDF:(NSString *)filePath withError:(NSError *_Nullable *)error;
- (NSDictionary *)exportXFDF:(NSString *)filePath withError:(NSError *_Nullable *)error;

/// Annotation Contextual Menu Customization
- (void)setAnnotationContextualMenuItems:(NSDictionary *)items;

@end

NS_ASSUME_NONNULL_END
