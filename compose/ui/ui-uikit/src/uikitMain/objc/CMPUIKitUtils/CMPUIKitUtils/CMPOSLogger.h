//
//  CMPOSLogger.h
//  CMPUIKitUtils
//
//  Created by Ilia.Semenov on 21/02/2024.
//

#import <Foundation/Foundation.h>

#import "CMPOSLoggerInterval.h"

NS_ASSUME_NONNULL_BEGIN

@interface CMPOSLogger : NSObject

- (instancetype)initWithCategoryName:(NSString *)name;
- (CMPOSLoggerInterval *)beginIntervalNamed:(NSString *)name;
- (void)endInterval:(CMPOSLoggerInterval *)interval;

@end

NS_ASSUME_NONNULL_END
