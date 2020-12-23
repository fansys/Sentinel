package com.alibaba.csp.sentinel.dashboard.netty.server;

import com.alibaba.csp.sentinel.command.CommandRequest;
import com.alibaba.csp.sentinel.command.CommandResponse;

/**
 * Represent a handler that handles a {@link CommandRequest}.
 *
 * @author Eric ZhaoServerCommandHandler
 */
public interface ServerCommandHandler<R> {

    /**
     * Handle the given Courier command request.
     *
     * @param request the request to handle
     * @return the response
     */
    CommandResponse<R> handle(CommandRequest request);
}
