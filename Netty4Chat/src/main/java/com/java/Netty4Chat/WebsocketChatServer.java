package com.java.Netty4Chat;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * 
 * @author jiemin
 *
 */
public class WebsocketChatServer {

	private int port;
	
	public WebsocketChatServer(int port) {
		this.port = port;
	}
	
	
	public void run() {
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		
		try{
		ServerBootstrap serverBootstrap = new ServerBootstrap();
		serverBootstrap.group(bossGroup, workerGroup)
		.channel(NioServerSocketChannel.class)
		.childHandler(new ChannelInitializer<SocketChannel>() {

			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ChannelPipeline pipeline = ch.pipeline();
				pipeline.addLast(new HttpServerCodec());
				pipeline.addLast(new HttpObjectAggregator(64*1024));
				pipeline.addLast(new ChunkedWriteHandler());
				pipeline.addLast(new HttpRequestHandler("/ws"));
				pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));
				pipeline.addLast(new TextWebSocketFrameHandler());
			}
		}).option(ChannelOption.SO_BACKLOG, 180)
		.childOption(ChannelOption.SO_KEEPALIVE, true);
		
		ChannelFuture  future = serverBootstrap.bind(port).sync();
		
		future.channel().closeFuture().sync();
		
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}
	
	
	public static void main(String[] args) {
		int port = 8080;
		new WebsocketChatServer(port).run();
	}

}
