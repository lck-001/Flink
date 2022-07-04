package com.data.ceph.utils;


import java.io.Serializable;

/**
 * @ClassName IPEntity
 * @Description TODO
 * @Author qgp
 * @Date 2019/9/24 17:50
 * @Version 1.0
 **/
public class IPEntity implements Serializable {

    //国家
    public String countryName;
    //国家代码
    public String countryCode;

    //省份
    public String provinceName;
    public String provinceCode;

    //城市名称
    public String cityName;

    //邮政编码
    public String postalCode;

    //经度
    public Double longitude;
    //纬度
    public Double latitude;

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public String getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(String provinceCode) {
        this.provinceCode = provinceCode;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }
}
