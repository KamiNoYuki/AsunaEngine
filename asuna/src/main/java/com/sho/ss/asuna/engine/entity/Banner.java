package com.sho.ss.asuna.engine.entity;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;

import java.util.Arrays;

public class Banner
{
    public static final String TYPE_SYSTEM = "SYSTEM";
    public static final String TYPE_OTHER = "OTHER";
    private final String[] typeSymbols = new String[]{TYPE_SYSTEM, TYPE_OTHER};
    /**
     * 该Banner引用的视频源，如果为空，则点击执行搜索，如果不为空，则进入观看页面
     */
    @Nullable
    private VideoSource source;
    private String imageUrl;
    private String title;
    private String subtitle;
    /**
     * 详情页链接，如果为空，则进入搜索界面根据title值进行搜索
     */
    private String dtUrl;
    /**
     * subType字段指明subTitle描述类型:
     * 1. SYSTEM(评分+上映时间) . 当subType指明为该类型，会对subTitle进行一些特殊处理
     * 2. OTHER(其他描述).     当subType指明为该类型(默认为该类型)，不对对其进行任何处理
     * 更多其它应用类型待定..
     **/
    private String subType = TYPE_OTHER;

    public Banner()
    {

    }

    public Banner(@Nullable VideoSource source, String imageUrl, String title, String subtitle, String subType)
    {
        this.source = source;
        this.imageUrl = imageUrl;
        this.title = title;
        this.subtitle = subtitle;
        setSubType(subType);
    }

    public Banner(String imageUrl, String title, String subtitle, String subType)
    {
        this(null, imageUrl, title, subtitle, subType);
    }

    public Banner(@Nullable VideoSource source, String imageUrl, String title, String subtitle, String dtUrl, String subType)
    {
        this.source = source;
        this.imageUrl = imageUrl;
        this.title = title;
        this.subtitle = subtitle;
        this.dtUrl = dtUrl;
        setSubType(subType);
    }

    public String getImageUrl()
    {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl)
    {
        this.imageUrl = imageUrl;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getSubtitle()
    {
        return subtitle;
    }

    public void setSubtitle(String subtitle)
    {
        this.subtitle = subtitle;
    }

    public String getSubType()
    {
        return subType;
    }

    public void setSubType(@NonNull String subType)
    {
        int matcher = 0;
        for (String typeSymbol : typeSymbols)
        {
            if (subType.equalsIgnoreCase(typeSymbol))
                matcher++;
        }
        if (matcher <= 0)
            throw new IllegalArgumentException("The value of the field subType is unknown!");
        else this.subType = subType.toUpperCase();
    }

    @Nullable
    public VideoSource getSource()
    {
        return source;
    }

    public void setSource(@Nullable VideoSource source)
    {
        this.source = source;
    }

    public String getDtUrl()
    {
        return dtUrl;
    }

    public void setDtUrl(String dtUrl)
    {
        this.dtUrl = dtUrl;
    }

    @NonNull
    public Video toVideo() {
        return new Video(imageUrl, null, dtUrl,
                null, null, title,
                subtitle, null, null,
                null, null, source);
    }

    @NonNull
    @Override
    public String toString()
    {
        return "Banner{" +
                "typeSymbols=" + Arrays.toString(typeSymbols) +
                ", source=" + source +
                ", imageUrl='" + imageUrl + '\'' +
                ", title='" + title + '\'' +
                ", subtitle='" + subtitle + '\'' +
                ", dtUrl='" + dtUrl + '\'' +
                ", subType='" + subType + '\'' +
                '}';
    }
}
