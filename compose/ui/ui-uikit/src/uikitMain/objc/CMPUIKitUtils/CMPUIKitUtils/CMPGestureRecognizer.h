//
//  CMPGestureRecognizer.h
//  CMPUIKitUtils
//
//  Created by Ilia.Semenov on 28/06/2024.
//

#import <UIKit/UIKit.h>
#import <UIKit/UIGestureRecognizerSubclass.h>
#import "CMPMacros.h"

NS_ASSUME_NONNULL_BEGIN

/// A class that reexports usual UIGestureRecognizerDelegate selectors without causing conflicting overload on Kotlin side, default implementations return the same result as if the selector wasn't implemented at all, following the documentation
/// See https://developer.apple.com/documentation/uikit/uigesturerecognizerdelegate
@interface CMPGestureRecognizerDelegateProxy : NSObject <UIGestureRecognizerDelegate>

- (BOOL)gestureRecognizerShouldRequireFailureOfGestureRecognizer:(UIGestureRecognizer *)gestureRecognizer otherGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer CMP_CAN_OVERRIDE;

- (BOOL)gestureRecognizerShouldRecognizeSimultaneouslyWithGestureRecognizer:(UIGestureRecognizer *)gestureRecognizer otherGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer CMP_CAN_OVERRIDE;

- (BOOL)gestureRecognizerShouldBeRequiredToFailByGestureRecognizer:(UIGestureRecognizer *)gestureRecognizer otherGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer CMP_CAN_OVERRIDE;

@end


@interface CMPGestureRecognizer : UIGestureRecognizer <UIGestureRecognizerDelegate>

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event CMP_ABSTRACT_FUNCTION;

- (void)touchesMoved:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event CMP_ABSTRACT_FUNCTION;

- (void)touchesEnded:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event CMP_ABSTRACT_FUNCTION;

- (void)touchesCancelled:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event CMP_ABSTRACT_FUNCTION;

@end


NS_ASSUME_NONNULL_END
 
