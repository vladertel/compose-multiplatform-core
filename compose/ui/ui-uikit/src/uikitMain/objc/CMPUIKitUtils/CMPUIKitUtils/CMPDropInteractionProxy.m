//
//  CMPDropInteractionProxy.m
//  CMPUIKitUtils
//
//  Created by Ilia.Semenov on 17/07/2024.
//

#import "CMPDropInteractionProxy.h"

NS_ASSUME_NONNULL_BEGIN

@implementation CMPDropInteractionProxy

- (BOOL)dropInteraction:(UIDropInteraction *)interaction canHandleSession:(id<UIDropSession>)session {
    return [self canHandleSession:session interaction:interaction];
}

- (BOOL)canHandleSession:(id<UIDropSession>)session interaction:(UIDropInteraction *)interaction {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (void)dropInteraction:(UIDropInteraction *)interaction performDrop:(id<UIDropSession>)session {
    [self performDropFromSession:session interaction:interaction];
}

- (void)performDropFromSession:(id<UIDropSession>)session interaction:(UIDropInteraction *)interaction {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

@end

@implementation UIDragItem (CMPUnpacking)

- (NSProgress *_Nullable)cmp_loadString:(void (^)(NSString *result, NSError *error))completionHandler {
    if ([self.itemProvider canLoadObjectOfClass:NSString.class]) {
        return [self.itemProvider loadObjectOfClass:NSString.class completionHandler:completionHandler];
    } else {
        return nil;
    }
}

@end

NS_ASSUME_NONNULL_END
