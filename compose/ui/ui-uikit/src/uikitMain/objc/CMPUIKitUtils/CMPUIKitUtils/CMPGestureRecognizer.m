//
//  CMPGestureRecognizer.m
//  CMPUIKitUtils
//
//  Created by Ilia.Semenov on 28/06/2024.
//

#import "CMPGestureRecognizer.h"

@implementation CMPGestureRecognizerDelegateProxy

- (BOOL)gestureRecognizer:(UIGestureRecognizer *)gestureRecognizer shouldRequireFailureOfGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer {
    return [self gestureRecognizerShouldRequireFailureOfGestureRecognizer:gestureRecognizer otherGestureRecognizer:otherGestureRecognizer];
}

- (BOOL)gestureRecognizerShouldRequireFailureOfGestureRecognizer:(UIGestureRecognizer *)gestureRecognizer otherGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer {
    return NO;
}

- (BOOL)gestureRecognizer:(UIGestureRecognizer *)gestureRecognizer shouldRecognizeSimultaneouslyWithGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer {
    return [self gestureRecognizerShouldRecognizeSimultaneouslyWithGestureRecognizer:gestureRecognizer otherGestureRecognizer:otherGestureRecognizer];
}

- (BOOL)gestureRecognizerShouldRecognizeSimultaneouslyWithGestureRecognizer:(UIGestureRecognizer *)gestureRecognizer otherGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer {
    return NO;
}

- (BOOL)gestureRecognizer:(UIGestureRecognizer *)gestureRecognizer shouldBeRequiredToFailByGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer {
    return [self gestureRecognizerShouldBeRequiredToFailByGestureRecognizer:gestureRecognizer otherGestureRecognizer:otherGestureRecognizer];
}

- (BOOL)gestureRecognizerShouldBeRequiredToFailByGestureRecognizer:(UIGestureRecognizer *)gestureRecognizer otherGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer {
    return NO;
}

@end


@implementation CMPGestureRecognizer

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (void)touchesMoved:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (void)touchesEnded:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (void)touchesCancelled:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    CMP_ABSTRACT_FUNCTION_CALLED
}

@end
