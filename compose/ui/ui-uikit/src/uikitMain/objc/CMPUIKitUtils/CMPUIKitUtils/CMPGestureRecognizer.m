//
//  CMPGestureRecognizer.m
//  CMPUIKitUtils
//
//  Created by Ilia.Semenov on 28/06/2024.
//

#import "CMPGestureRecognizer.h"

@implementation CMPGestureRecognizer {
    dispatch_block_t _scheduledFailureBlock;
}

- (instancetype)init {
    self = [super init];
    
    if (self) {        
        self.delegate = self;
        [self addTarget:self action:@selector(handleStateChange)];
    }
    
    return self;
}

- (void)handleStateChange {
    switch (self.state) {
        case UIGestureRecognizerStateEnded:
        case UIGestureRecognizerStateCancelled:
            [self cancelFailure];
            break;

        default:
            break;
    }
}

- (BOOL)gestureRecognizer:(UIGestureRecognizer *)gestureRecognizer shouldRequireFailureOfGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer {
    NSLog(@"is not required to fail %@", otherGestureRecognizer);
    
    return NO;
}

- (BOOL)gestureRecognizer:(UIGestureRecognizer *)gestureRecognizer shouldRecognizeSimultaneouslyWithGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer {
    UIView *view = self.view;
    UIView *otherView = otherGestureRecognizer.view;
    
    NSLog(@"%@", otherGestureRecognizer);
    
    if (view == nil || otherView == nil) {
        return NO;
    }
    
    if (otherView == view) {
        NSLog(@"is allowed to run with %@", otherGestureRecognizer);
        return YES;
    }
    
    // Allow simultaneous recognition only if otherGestureRecognizer is attached to the view up in the hierarchy
    return ![otherView isDescendantOfView:view];
    
}

- (BOOL)gestureRecognizer:(UIGestureRecognizer *)gestureRecognizer shouldBeRequiredToFailByGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer {
    if (otherGestureRecognizer.view == self.view) {
        NSLog(@"is not required to be failed by %@", otherGestureRecognizer);
        return NO;
    } else {
        return YES;
    }
}

- (void)cancelFailure {
    if (_scheduledFailureBlock) {
        dispatch_block_cancel(_scheduledFailureBlock);
        _scheduledFailureBlock = NULL;
    }
}

- (void)fail {
    [self.handler onFailure];
}

- (void)scheduleFailure {
    __weak typeof(self) weakSelf = self;
    dispatch_block_t dispatchBlock = dispatch_block_create(0, ^{
        [weakSelf fail];
    });
    
    if (_scheduledFailureBlock) {
        dispatch_block_cancel(_scheduledFailureBlock);
    }
    _scheduledFailureBlock = dispatchBlock;
    
    // 150ms is a timer delay for notifying a handler that the gesture was failed to recognize.
    // `handler` implementtion is responsible for cancelling this via calling `cancelFailure` and transitioning
    // this gesture recognizer to a proper state.
    double failureDelay = 0.15;
    
    dispatch_time_t dispatchTime = dispatch_time(DISPATCH_TIME_NOW, (int64_t)(failureDelay * NSEC_PER_SEC));

    // Schedule the block to be executed at `dispatchTime`
    dispatch_after(dispatchTime, dispatch_get_main_queue(), dispatchBlock);
}

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self.handler touchesBegan:touches withEvent:event];
}

- (void)touchesMoved:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self.handler touchesMoved:touches withEvent:event];
}

- (void)touchesEnded:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self.handler touchesEnded:touches withEvent:event];
}

- (void)touchesCancelled:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self.handler touchesCancelled:touches withEvent:event];
}

@end
