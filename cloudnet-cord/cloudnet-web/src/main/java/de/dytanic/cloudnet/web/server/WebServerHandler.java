/*
 * Copyright (c) Tarek Hosni El Alaoui 2017
 */

package de.dytanic.cloudnet.web.server;

import de.dytanic.cloudnet.lib.map.WrappedMap;
import de.dytanic.cloudnet.web.server.handler.WebHandler;
import de.dytanic.cloudnet.web.server.util.PathProvider;
import de.dytanic.cloudnet.web.server.util.QueryDecoder;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.net.URI;
import java.util.List;

/**
 * Created by Tareko on 14.09.2017.
 */
final class WebServerHandler extends ChannelInboundHandlerAdapter {

    private WebServer webServer;

    public WebServerHandler(WebServer webServer) {
        this.webServer = webServer;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof HttpRequest)) return;
        HttpRequest httpRequest = ((HttpRequest) msg);

        URI uri = new URI(httpRequest.getUri());
        String path = uri.getRawPath();
        if (path == null) {
            path = "/";
        }

        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        List<WebHandler> webHandlers = webServer.getWebServerProvider().getHandlers(path);
        if (webHandlers.size() != 0) {
            FullHttpResponse fullHttpResponse = null;
            for (WebHandler webHandler : webHandlers) {
                if (path.isEmpty() || path.equals("/"))
                    fullHttpResponse = webHandler.handleRequest(ctx, new QueryDecoder(uri.getQuery()), new PathProvider(path, new WrappedMap()), httpRequest);
                else {
                    String[] array = path.replaceFirst("/", "").split("/");
                    String[] pathArray = webHandler.getPath().replaceFirst("/", "").split("/");
                    WrappedMap wrappedMap = new WrappedMap();
                    for (short i = 0; i < array.length; i++) {
                        if (pathArray[i].startsWith("{") && pathArray[i].endsWith("}")) {
                            wrappedMap.append(pathArray[i].replace("{", "").replace("}", ""), array[i]);
                        }
                    }
                    fullHttpResponse = webHandler.handleRequest(ctx, new QueryDecoder(uri.getQuery()), new PathProvider(path, wrappedMap), httpRequest);
                }
            }
            if (fullHttpResponse == null) {
                fullHttpResponse = new DefaultFullHttpResponse(httpRequest.getProtocolVersion(), HttpResponseStatus.NOT_FOUND, Unpooled.wrappedBuffer("Error 404 page not found!".getBytes()));
            }
            fullHttpResponse.headers().set("Access-Control-Allow-Origin", "*");
            ctx.writeAndFlush(fullHttpResponse).addListener(ChannelFutureListener.CLOSE);
        } else {
            FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(httpRequest.getProtocolVersion(), HttpResponseStatus.NOT_FOUND);
            fullHttpResponse.headers().set("Access-Control-Allow-Origin", "*");
            ctx.writeAndFlush(fullHttpResponse).addListener(ChannelFutureListener.CLOSE);
        }
    }
}