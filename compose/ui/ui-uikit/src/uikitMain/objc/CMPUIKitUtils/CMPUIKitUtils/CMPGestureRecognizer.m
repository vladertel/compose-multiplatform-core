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
        self.cancelsTouchesInView = true;
        self.delaysTouchesBegan = true;
        self.delegate = self;
        
        _scheduledFailureBlock = NULL;
    }
    
    return self;
}

- (BOOL)gestureRecognizer:(UIGestureRecognizer *)gestureRecognizer shouldRecognizeSimultaneouslyWithGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer {
    id <CMPGestureRecognizerHandler> handler = self.handler;
    
    if (handler) {
        return [handler shouldRecognizeSimultaneously:gestureRecognizer withOther:otherGestureRecognizer];
    } else {
        return NO;
    }
}

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self.handler touchesBegan:touches withEvent:event];
}

- (void)begin {
    if (self.state == UIGestureRecognizerStatePossible) {
        self.state = UIGestureRecognizerStateBegan;
        
        if (_scheduledFailureBlock) {
            dispatch_block_cancel(_scheduledFailureBlock);
            _scheduledFailureBlock = NULL;
        }
    }
}

- (void)fail {
    self.state = UIGestureRecognizerStateFailed;
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

    // Calculate the delay time in dispatch_time_t
    dispatch_time_t delay = dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.15 * NSEC_PER_SEC));

    // Schedule the block to be executed after the delay on the main queue
    dispatch_after(delay, dispatch_get_main_queue(), dispatchBlock);
}

- (void)touchesMoved:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self.handler touchesMoved:touches withEvent:event];
    
    switch (self.state) {
        case UIGestureRecognizerStateBegan:
        case UIGestureRecognizerStateChanged:
            self.state = UIGestureRecognizerStateChanged;
            break;
        default:
            break;
    }
}

- (void)touchesEnded:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self.handler touchesEnded:touches withEvent:event];
    
    switch (self.state) {
        case UIGestureRecognizerStateBegan:
        case UIGestureRecognizerStateChanged:
            if (self.numberOfTouches == 0) {
                self.state = UIGestureRecognizerStateEnded;
            } else {
                self.state = UIGestureRecognizerStateChanged;
            }
            break;
        default:
            break;
    }
}

- (void)touchesCancelled:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self.handler touchesCancelled:touches withEvent:event];
    
    switch (self.state) {
        case UIGestureRecognizerStateBegan:
        case UIGestureRecognizerStateChanged:
            if (self.numberOfTouches == 0) {
                self.state = UIGestureRecognizerStateCancelled;
            } else {
                self.state = UIGestureRecognizerStateChanged;
            }
            break;
        default:
            break;
    }
}

@end
