//
//  CMPAccessibilityContainer.m
//  CMPUIKitUtils
//
//  Created by Ilia.Semenov on 06/12/2023.
//

#import "CMPAccessibilityContainer.h"

NS_ASSUME_NONNULL_BEGIN

@implementation CMPAccessibilityContainer


-(instancetype)initWithAccessibilityContainer:(id)container {
    self = [super initWithAccessibilityContainer:container];
    
    if (self) {
        self.children = [NSMutableArray new];
    }
    
    return self;
}

- (NSInteger)accessibilityElementCount {
    return self.children.count + 1;
}

- (NSInteger)indexOfAccessibilityElement:(nonnull id)element {
    if (element == self.wrappedElement) {
        return 0;
    }
    
    NSInteger index = [self.children indexOfObject:element];
    if (index == NSNotFound) {
        
    }
    
}

- (nullable id)accessibilityElementAtIndex:(NSInteger)index {
    return self;
}

@end

NS_ASSUME_NONNULL_END
