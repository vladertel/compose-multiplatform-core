//
//  CMPDropInteractionProxy.h
//  CMPUIKitUtils
//
//  Created by Ilia.Semenov on 17/07/2024.
//

#import <UIKit/UIKit.h>
#import "CMPMacros.h"

NS_ASSUME_NONNULL_BEGIN

@interface CMPDropInteractionProxy : NSObject <UIDropInteractionDelegate>

- (BOOL)canHandleSession:(id<UIDropSession>)session interaction:(UIDropInteraction *)interaction CMP_MUST_BE_OVERRIDED;
- (void)performDropFromSession:(id<UIDropSession>)session interaction:(UIDropInteraction *)interaction CMP_MUST_BE_OVERRIDED;

@end

NS_ASSUME_NONNULL_END
