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

@interface CMPGestureRecognizer : UIGestureRecognizer <UIGestureRecognizerDelegate>

- (instancetype)initWithTarget:(id _Nullable)target action:(SEL _Nullable)action NS_DESIGNATED_INITIALIZER;

- (void)handleStateChange CMP_CAN_OVERRIDE;

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event CMP_ABSTRACT_FUNCTION;

- (void)touchesMoved:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event CMP_ABSTRACT_FUNCTION;

- (void)touchesEnded:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event CMP_ABSTRACT_FUNCTION;

- (void)touchesCancelled:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event CMP_ABSTRACT_FUNCTION;

- (BOOL)gestureRecognizerShouldRequireFailureOfGestureRecognizer:(UIGestureRecognizer *)gestureRecognizer otherGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer CMP_CAN_OVERRIDE;

- (BOOL)gestureRecognizerShouldRecognizeSimultaneouslyWithGestureRecognizer:(UIGestureRecognizer *)gestureRecognizer otherGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer CMP_CAN_OVERRIDE;

- (BOOL)gestureRecognizerShouldBeRequiredToFailByGestureRecognizer:(UIGestureRecognizer *)gestureRecognizer otherGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer CMP_CAN_OVERRIDE;

@end


NS_ASSUME_NONNULL_END
 
