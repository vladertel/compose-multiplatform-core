//
//  CMPAccessibilityContainer.m
//  CMPUIKitUtils
//
//  Created by Ilia.Semenov on 06/12/2023.
//

#import "CMPAccessibilityContainer.h"

NS_ASSUME_NONNULL_BEGIN

@implementation CMPAccessibilityContainer

-(NSInteger)accessibilityElementCount {
    assert(false && "MUST_OVERRIDE");
}

-(NSInteger)indexOfAccessibilityElement:(nonnull id)element {
    assert(false && "MUST_OVERRIDE");
}

-(nullable id)accessibilityElementAtIndex:(NSInteger)index {
    assert(false && "MUST_OVERRIDE");
}

@end

NS_ASSUME_NONNULL_END
