//
//  CMPDropInteractionProxy.m
//  CMPUIKitUtils
//
//  Created by Ilia.Semenov on 17/07/2024.
//

#import "CMPDropInteractionProxy.h"

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

- (void)performDropFromSession:(nonnull id<UIDropSession>)session interaction:(nonnull UIDropInteraction *)interaction {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

@end
