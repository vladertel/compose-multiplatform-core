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
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (void)dropInteraction:(UIDropInteraction *)interaction performDrop:(id<UIDropSession>)session {
    [self performDropFromSession:session interaction:interaction];
}

- (void)performDropFromSession:(id<UIDropSession>)session interaction:(UIDropInteraction *)interaction {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (UIDropProposal *)dropInteraction:(UIDropInteraction *)interaction sessionDidUpdate:(id<UIDropSession>)session {
    return [self proposalForSessionUpdate:session interaction:interaction];
}

- (UIDropProposal *)proposalForSessionUpdate:(id<UIDropSession>)session interaction:(UIDropInteraction *)interaction {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (void)dropInteraction:(UIDropInteraction *)interaction sessionDidEnd:(id<UIDropSession>)session {
    return [self sessionDidEnd:session interaction:interaction];
}

- (void)sessionDidEnd:(id<UIDropSession>)session interaction:(UIDropInteraction *)interaction {
    CMP_ABSTRACT_FUNCTION_CALLED
}

@end

@implementation UIDragItem (CMPLoading)

- (void)cmp_loadString:(void (^)(NSString  * _Nullable result, NSError *_Nullable error))completionHandler {
    if ([self.itemProvider canLoadObjectOfClass:NSString.class]) {
        [self.itemProvider loadObjectOfClass:NSString.class completionHandler:completionHandler];
    } else {
        completionHandler(nil, nil);
    }
}

- (void)cmp_loadAny:(Class)objectClass onCompletion:(void (^)(id _Nullable result, NSError *_Nullable error))completionHandler {
    if (![objectClass conformsToProtocol:@protocol(NSItemProviderReading)]) {
        @throw [[NSException alloc] initWithName:@"UIDragItemLoadingError" reason:[NSString stringWithFormat:@"%@ doesn't conform to NSItemProviderReading", objectClass] userInfo:nil];
    } else if ([self.itemProvider canLoadObjectOfClass:objectClass]) {
        [self.itemProvider loadObjectOfClass:objectClass completionHandler:completionHandler];
    } else {
        completionHandler(nil, nil);
    }
}

@end

NS_ASSUME_NONNULL_END
