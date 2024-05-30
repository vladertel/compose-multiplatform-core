/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#import "CMPGestureRecognizer.h"

@implementation CMPGestureRecognizer {
    NSInteger _touchesCount;
}

- (instancetype)init {
    self = [super init];
    
    if (self) {
        self.cancelsTouchesInView = NO;
        self.delegate = self;
        _touchesCount = 0;
    }
    
    return self;
}

- (BOOL)gestureRecognizer:(UIGestureRecognizer *)gestureRecognizer shouldRecognizeSimultaneouslyWithGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer {
    if (_proxy) {
        return [_proxy shouldRecognizeSimultaneously:gestureRecognizer withOtherGestureRecognizer:otherGestureRecognizer];
    } else {
        return NO;
    }
}

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    _touchesCount += touches.count;
    
    if (self.state == UIGestureRecognizerStatePossible) {
        self.state = UIGestureRecognizerStateBegan;
    }
    
    [self.proxy touchesBegan:touches withEvent:event];
}

- (void)touchesMoved:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    self.state = UIGestureRecognizerStateChanged;
    
    [self.proxy touchesMoved:touches withEvent:event];
}

- (void)touchesCancelled:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    self.state = UIGestureRecognizerStateCancelled;
    
    [self.proxy touchesCancelled:touches withEvent:event];
}

- (void)touchesEnded:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    _touchesCount -= touches.count;
    
    if (_touchesCount == 0) {
        self.state = UIGestureRecognizerStateEnded;
    }
    
    [self.proxy touchesEnded:touches withEvent:event];
}

- (void)reset {
    _touchesCount = 0; // in case cancellation happened
    self.state = UIGestureRecognizerStatePossible;
}

@end

/**
 var touchCallback: ((ForwardingGestureRecognizer, Set<UITouch>, UIEvent) -> Void)?

     // Initialization
     override init(target: Any?, action: Selector?) {
         super.init(target: target, action: action)
         self.cancelsTouchesInView = false // Ensure it does not block other recognizers/views
     }
     
     // Called when touches begin
     override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent) {
         super.touchesBegan(touches, with: event)
         touchCallback?(self, touches, event)
         state = .began // Begin the gesture
     }

     // Called when touches move
     override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent) {
         super.touchesMoved(touches, with: event)
         touchCallback?(self, touches, event)
         state = .changed // Update the gesture state
     }

     // Called when touches end
     override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent) {
         super.touchesEnded(touches, with: event)
         touchCallback?(self, touches, event)
         state = .ended // End the gesture
     }

     // Called when touches are cancelled
     override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent) {
         super.touchesCancelled(touches, with: event)
         touchCallback?(self, touches, event)
         state = .cancelled // Cancel the gesture
     }
     
     // Ensure this recognizer can coexist with other recognizers
     override func shouldRecognizeSimultaneouslyWith(_ otherGestureRecognizer: UIGestureRecognizer) -> Bool {
         return true
     }
 */
