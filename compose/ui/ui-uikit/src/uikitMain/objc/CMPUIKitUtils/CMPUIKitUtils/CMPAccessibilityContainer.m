//
//  CMPAccessibilityContainer.m
//  CMPUIKitUtils
//
//  Created by Ilia.Semenov on 06/12/2023.
//

#import "CMPAccessibilityContainer.h"
#import "CMPAccessibilityMacros.h"

NS_ASSUME_NONNULL_BEGIN

@implementation CMPAccessibilityContainer

// MARK: overrided UIAccessibilityContainer methods

- (NSInteger)accessibilityElementCount {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (NSInteger)indexOfAccessibilityElement:(id)element {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (__nullable id)accessibilityElementAtIndex:(NSInteger)index {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

@end

NS_ASSUME_NONNULL_END
