//
//  CMPMetalDrawableManager.h
//  CMPUIKitUtils
//
//  Created by Ilia.Semenov on 27/02/2024.
//

#import <Foundation/Foundation.h>
#import <QuartzCore/QuartzCore.h>

NS_ASSUME_NONNULL_BEGIN

@interface CMPMetalDrawableManager : NSObject

- (instancetype)initWithMetalLayer:(CAMetalLayer *)metalLayer;

@end

NS_ASSUME_NONNULL_END
