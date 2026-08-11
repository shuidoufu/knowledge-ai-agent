package com.example.aiagent.constant;

import java.util.List;

/**
 * 防盗链素材站黑名单
 * 搜索过滤（ImageSearchTool）与图片代理下载拦截（ImageProxyService）共用
 * 这些站点的图片无法通过任何请求头组合正常下载（403 或返回防盗链占位图），
 * 黑名单需同时收录主站域名与图片 CDN 域名（Bing 返回的图片 URL 通常托管在 CDN 域名），
 * 如遇到新的防盗链站点，追加域名即可
 */
public final class HotlinkImageConfig {

    private static final List<String> BLOCKED_HOSTS = List.of(
            "nipic.com",          // 昵图网：返回"昵图网防盗链"占位图
            "ntimg.cn",           // 昵图网图片 CDN（Bing 返回的昵图网图片托管域名）
            "51wendang.com",      // 道客巴巴：403 硬拒
            "dfic.cn",            // 图虫
            "veer.com",           // 海洛创意
            "quanjing.com",       // 全景网
            "vcg.com",            // 视觉中国
            "58pic.com",          // 千图网
            "zcool.com.cn"        // 站酷
    );

    private HotlinkImageConfig() {
    }

    /**
     * 判断图片 URL 是否属于防盗链黑名单站点（大小写不敏感，覆盖子域名与 CDN 域名）
     *
     * @param url 图片 URL
     * @return 是否应拦截
     */
    public static boolean isBlockedUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String lowerUrl = url.toLowerCase();
        return BLOCKED_HOSTS.stream().anyMatch(lowerUrl::contains);
    }

}
