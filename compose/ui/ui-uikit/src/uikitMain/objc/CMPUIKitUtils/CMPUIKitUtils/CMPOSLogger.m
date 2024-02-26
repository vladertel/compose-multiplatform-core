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
    CMPOSLoggerInterval *interval;
    
    [_poolLock lock];
    
    if (_intervalsPool.count > 0) {
        interval = _intervalsPool.lastObject;
        [_intervalsPool removeLastObject];
    } else {
        interval = nil;
    }
    
    [_poolLock unlock];
    
    if (interval) {
        [interval beginWithName:name];
        return interval;
    } else {
        interval = [[CMPOSLoggerInterval alloc] initWithLog:_log];
        [interval beginWithName:name];
        return interval;
    }
}

- (void)endInterval:(CMPOSLoggerInterval *)interval {
    [interval end];
    [_poolLock lock];
    
    [_intervalsPool addObject:interval];
    
    [_poolLock unlock];
}


@end
