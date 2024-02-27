//
//  CMPMetalDrawableManager.m
//  CMPUIKitUtils
//
//  Created by Ilia.Semenov on 27/02/2024.
//

#import "CMPMetalDrawableManager.h"

@implementation CMPMetalDrawableManager {
    CAMetalLayer *_metalLayer;
    id <CAMetalDrawable> _drawable;
}

- (instancetype)initWithMetalLayer:(CAMetalLayer *)metalLayer {
    self = [super init];
    
    if (self) {
        _metalLayer = metalLayer;
        _drawable = nil;
    }
    
    return self;
}

- (BOOL)acquireNextDrawable {
    assert(_drawable == nil);
    
    _drawable = [_metalLayer nextDrawable];
    
    return _drawable != nil;
}

/// Override getter implementation for `texture` property
- (void *)texture {
    assert(_drawable != nil);
    
    return (__bridge void *)_drawable.texture;
}

- (void)presentInCommandBuffer:(id <MTLCommandBuffer>)commandBuffer {
    assert(_drawable != nil);
    
    [commandBuffer presentDrawable:_drawable];
}

- (void)present {
    assert(_drawable != nil);
    
    [_drawable present];
}

- (void)releaseDrawable {
    assert(_drawable != nil);
    
    _drawable = nil;
}

@end
