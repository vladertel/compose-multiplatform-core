//
//  CMPOSLogger.m
//  CMPUIKitUtils
//
//  Created by Ilia.Semenov on 21/02/2024.
//

#import "CMPOSLogger.h"

@implementation CMPOSLogger {
    os_log_t _log;
    NSMutableArray<CMPOSLoggerInterval *> *_intervalsPool;
    NSLock *_poolLock;
}

- (instancetype)initWithCategoryName:(NSString *)name {
    self = [super init];
    
    if (self) {
        _log = os_log_create("androidx.compose", [name cStringUsingEncoding:NSUTF8StringEncoding]);
        _intervalsPool = [NSMutableArray new];
        _poolLock = [NSLock new];
    }
    
    return self;
}

- (CMPOSLoggerInterval *)beginIntervalNamed:(NSString *)name {
    //return [CMPOSLoggerInterval new];
    return nil;
}

- (void)endInterval:(CMPOSLoggerInterval *)interval {
    
}


@end
