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

- (void)dropInteraction:(UIDropInteraction *)interaction sessionDidEnter:(id<UIDropSession>)session {
    [self sessionDidEnter:session interaction:interaction];
}

- (void)sessionDidEnter:(id<UIDropSession>)session interaction:(UIDropInteraction *)interaction {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (void)dropInteraction:(UIDropInteraction *)interaction sessionDidExit:(id<UIDropSession>)session {
    [self sessionDidExit:session interaction:interaction];
}

- (void)sessionDidExit:(id<UIDropSession>)session interaction:(UIDropInteraction *)interaction {
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

- (void)cmp_loadAny:(Class)objectClass onCompletion:(void (^)(NSObject *_Nullable result, NSError *_Nullable error))completionHandler {
    if (![objectClass conformsToProtocol:@protocol(NSItemProviderReading)]) {
        NSDictionary *userInfo = @{
            @"description" : [NSString stringWithFormat:@"%@ doesn't conform to protocol NSItemProviderReading and thus can't be loaded", objectClass.description]
        };
        
        NSError *error = [NSError errorWithDomain:NSCocoaErrorDomain
                                             code:0
                                         userInfo:userInfo];
        completionHandler(nil, error);                    
    } else if ([self.itemProvider canLoadObjectOfClass:objectClass]) {
        [self.itemProvider loadObjectOfClass:objectClass completionHandler:completionHandler];
    } else {
        completionHandler(nil, nil);
    }
}

@end

NS_ASSUME_NONNULL_END
