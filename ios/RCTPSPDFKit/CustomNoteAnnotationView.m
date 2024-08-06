
#import <Foundation/Foundation.h>
#import "CustomNoteAnnotationView.h"

@implementation CustomNoteAnnotationView

- (UIImage *)renderNoteImage {
  NSDictionary<NSString *, id> *customData = self.annotation.customData;
  id maybeVectorStampAsset = [customData objectForKey:@"vectorStampAsset"];
  if ([maybeVectorStampAsset isKindOfClass:[NSString class]]) {
    NSString *vectorStampAsset = (NSString*)maybeVectorStampAsset;
    return [UIImage imageNamed:vectorStampAsset];
  }

  return [UIImage imageNamed:@"VerortungPin.pdf"];
}

@end
