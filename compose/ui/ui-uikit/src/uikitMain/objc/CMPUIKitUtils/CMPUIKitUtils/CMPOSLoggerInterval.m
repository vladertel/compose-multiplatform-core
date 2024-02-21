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
    os_signpost_id_t _signpost_id;
    const char *_lastName;
}

- (instancetype)initWithLog:(os_log_t)log {
    self = [super init];
    
    if (self) {
        _log = log;
        _signpost_id = os_signpost_id_generate(_log);
        _lastName = NULL;
    }
    
    return self;
}

@end
