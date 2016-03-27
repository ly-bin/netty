package com.java.Netty4Chat;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.net.URL;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedNioFile;

/**
 * 处理http 请求
 * @author jiemin
 * @date 2016-03-27
 */
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
	private final String URI;
	private static final File INDEX;
	
	static {
		URL location = HttpRequestHandler.class.getProtectionDomain().getCodeSource().getLocation();
        try {
            String path = location.toURI() + "WebsocketChatClient.html";
            path = !path.contains("file:") ? path : path.substring(5);
            INDEX = new File(path);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("找不到WebsocketChatClient.html", e);
        }
	}
	
	public HttpRequestHandler(String URI) {
		this.URI = URI;
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
		if(URI.equalsIgnoreCase(msg.getUri())) {
			ctx.fireChannelRead(msg.retain());
		} else {
			if(HttpHeaders.is100ContinueExpected(msg)) {
				send100Continue(ctx);
			}
			
			RandomAccessFile file = new RandomAccessFile(INDEX, "r"); 
			HttpResponse response = new DefaultHttpResponse(msg.getProtocolVersion(), HttpResponseStatus.OK);
			
			response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/html; charset=UTF-8");
			
			boolean keepAlive = HttpHeaders.isKeepAlive(msg);
			if (keepAlive) {
				response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, file.length());
				response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
			}
			
			ctx.write(response);
			
			if (ctx.pipeline().get(SslHandler.class) == null) {
				ctx.write(new DefaultFileRegion(file.getChannel(), 0, file.length()));
			} else {
				ctx.write(new ChunkedNioFile(file.getChannel()));
			}
			ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
			if (!keepAlive) {
				future.addListener(ChannelFutureListener.CLOSE);
			}
			
			file.close();
		}
	}
	
	private static void send100Continue(ChannelHandlerContext ctx) {
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
		ctx.writeAndFlush(response);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		Channel incoming = ctx.channel();
		System.out.println("client:" + incoming.remoteAddress() + "出现异常");
		// 出现了异常就关闭连接
		cause.printStackTrace();
		ctx.close();
	}

}
