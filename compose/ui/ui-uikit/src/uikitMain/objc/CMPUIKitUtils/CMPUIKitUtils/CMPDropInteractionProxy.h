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

- (BOOL)canHandleSession:(id<UIDropSession>)session interaction:(UIDropInteraction *)interaction CMP_ABSTRACT_FUNCTION;

- (void)performDropFromSession:(id<UIDropSession>)session interaction:(UIDropInteraction *)interaction CMP_ABSTRACT_FUNCTION;

- (UIDropProposal *)proposalForSessionUpdate:(id<UIDropSession>)session interaction:(UIDropInteraction *)interaction CMP_ABSTRACT_FUNCTION;

- (void)sessionDidEnd:(id<UIDropSession>)session interaction:(UIDropInteraction *) interaction CMP_ABSTRACT_FUNCTION;

- (void)sessionDidEnter:(id<UIDropSession>)session interaction:(UIDropInteraction *)interaction CMP_ABSTRACT_FUNCTION;

- (void)sessionDidExit:(id<UIDropSession>)session interaction:(UIDropInteraction *)interaction CMP_ABSTRACT_FUNCTION;

@end

@interface UIDragItem (CMPLoading)

- (void)cmp_loadString:(void (^)(NSString  * _Nullable result, NSError *_Nullable error))completionHandler;

- (void)cmp_loadAny:(Class)objectClass onCompletion:(void (^)(NSObject *_Nullable result, NSError *_Nullable error))completionHandler;

@end

NS_ASSUME_NONNULL_END
