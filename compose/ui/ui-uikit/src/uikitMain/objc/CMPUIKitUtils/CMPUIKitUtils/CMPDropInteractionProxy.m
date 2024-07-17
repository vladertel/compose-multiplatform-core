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

- (UIDropProposal *)dropInteraction:(UIDropInteraction *)interaction sessionDidUpdate:(id<UIDropSession>)session {
    return [self proposalForSessionUpdate:session interaction:interaction];
}

- (UIDropProposal *)proposalForSessionUpdate:(id<UIDropSession>)session interaction:(UIDropInteraction *)interaction {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

@end

@implementation UIDragItem (CMPLoading)

- (BOOL)cmp_loadString:(void (^)(NSString *result, NSError *error))completionHandler {
    if ([self.itemProvider canLoadObjectOfClass:NSString.class]) {
        [self.itemProvider loadObjectOfClass:NSString.class completionHandler:completionHandler];
        return YES;
    } else {
        return NO;
    }
}

- (CMPDragItemLoadRequestResult)cmp_loadAny:(Class)objectClass onCompletion:(void (^)(id result, NSError *error))completionHandler {
    if (![objectClass conformsToProtocol:@protocol(NSItemProviderReading)]) {
        return CMPDragItemLoadRequestResultUnsupportedType;
    } else if ([self.itemProvider canLoadObjectOfClass:objectClass]) {
        [self.itemProvider loadObjectOfClass:objectClass completionHandler:completionHandler];
        return CMPDragItemLoadRequestResultSuccess;
    } else {
        return CMPDragItemLoadRequestResultWrongType;
    }
}

@end

NS_ASSUME_NONNULL_END
