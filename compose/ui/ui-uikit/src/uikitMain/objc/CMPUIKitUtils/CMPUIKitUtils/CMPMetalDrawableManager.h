//
//  CMPMetalDrawableManager.h
//  CMPUIKitUtils
//
//  Created by Ilia.Semenov on 27/02/2024.
//

#import <Foundation/Foundation.h>
#import <QuartzCore/QuartzCore.h>
#import <Metal/Metal.h>

NS_ASSUME_NONNULL_BEGIN

@interface CMPMetalDrawableManager : NSObject

@property(readonly, nonatomic) void *texture;

- (instancetype)initWithMetalLayer:(CAMetalLayer *)metalLayer;
- (BOOL)acquireNextDrawable;
- (void)presentInCommandBuffer:(id <MTLCommandBuffer>)commandBuffer;
- (void)present;
- (void)releaseDrawable;

@end

NS_ASSUME_NONNULL_END
