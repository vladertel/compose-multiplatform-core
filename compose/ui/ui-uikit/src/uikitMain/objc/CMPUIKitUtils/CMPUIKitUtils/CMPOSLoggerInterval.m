//
//  CMPOSLoggerInterval.m
//  CMPUIKitUtils
//
//  Created by Ilia.Semenov on 21/02/2024.
//

#import "CMPOSLoggerInterval.h"

#import <os/signpost.h>

@implementation CMPOSLoggerInterval {
    os_log_t _log;
    os_signpost_id_t _signpostId;
    NSString *_name;
}

- (instancetype)initWithLog:(os_log_t)log {
    self = [super init];
    
    if (self) {
        _log = log;
        _signpostId = os_signpost_id_generate(_log);
        _name = nil;
    }
    
    return self;
}

- (void)beginWithName:(NSString *)name {
    _name = name;
    
    os_signpost_interval_begin(_log, _signpostId, "interval", "name: %{public}s", [name UTF8String]);
}

- (void)end {
    os_signpost_interval_end(_log, _signpostId, "interval", "name: %{public}s", [_name UTF8String]);
}

@end
