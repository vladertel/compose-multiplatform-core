//
//  CMPOSLoggerInterval.h
//  CMPUIKitUtils
//
//  Created by Ilia.Semenov on 21/02/2024.
//

#import <Foundation/Foundation.h>
#import <os/log.h>

NS_ASSUME_NONNULL_BEGIN

@interface CMPOSLoggerInterval : NSObject

- (instancetype)initWithLog:(os_log_t)log;
- (void)beginWithName:(NSString *)name;
- (void)end;

@end

NS_ASSUME_NONNULL_END
