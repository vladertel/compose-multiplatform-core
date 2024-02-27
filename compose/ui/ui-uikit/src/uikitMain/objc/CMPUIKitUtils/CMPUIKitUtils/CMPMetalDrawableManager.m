//
//  CMPMetalDrawableManager.m
//  CMPUIKitUtils
//
//  Created by Ilia.Semenov on 27/02/2024.
//

#import "CMPMetalDrawableManager.h"

@implementation CMPMetalDrawableManager {
    CAMetalLayer *_metalLayer;
}

- (instancetype)initWithMetalLayer:(CAMetalLayer *)metalLayer {
    self = [super init];
    
    if (self) {
        _metalLayer = metalLayer;
    }
    
    return self;
}

@end
