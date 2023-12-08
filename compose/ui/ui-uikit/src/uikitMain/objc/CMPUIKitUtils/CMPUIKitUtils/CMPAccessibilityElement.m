/*
 * Copyright 2022 The Android Open Source Project
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

#import "CMPAccessibilityElement.h"
#import "CMPAccessibilityBridge.h"

NS_ASSUME_NONNULL_BEGIN

@implementation CMPAccessibilityElement {
    BOOL _inDealloc;
    __weak id<CMPAccessibilityBridge> _bridge;
}

- (id)initWithBridge:(id<CMPAccessibilityBridge>)bridge {
    self = [super initWithAccessibilityContainer:bridge.container];
    
    if (self) {
        _inDealloc = NO;
        _bridge = bridge;
    }
    
    return self;
}

- (void)dealloc {
    _inDealloc = YES;
}

- (void)setAccessibilityContainer:(__nullable id)accessibilityContainer {
    // NoOp
}

- (__nullable id)accessibilityContainer {
    if (_inDealloc) {
        return nil;
    }
    
    if (_bridge) {
        if (!_bridge.isAlive) {
            return nil;
        }
    } else {
        return nil;
    }
    
    return [self resolveAccessibilityContainer];
}

- (__nullable id)resolveAccessibilityContainer {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (id)actualAccessibilityElement {
    return self;
}

@end

NS_ASSUME_NONNULL_END
