//
//  CMPAccessibilityBridge.h
//  CMPUIKitUtils
//
//  Created by Ilia.Semenov on 08/12/2023.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@protocol CMPAccessibilityBridge <NSObject>

- (id)container;
- (BOOL)isAlive;

@end

NS_ASSUME_NONNULL_END
