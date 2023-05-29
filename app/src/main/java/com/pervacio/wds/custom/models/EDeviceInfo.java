package com.pervacio.wds.custom.models;

/**
 * Created by: Darpan Dodiya on 5/24/2017.
 * Contributors:
 * Last updated on: 5/26/2017
 */

public class EDeviceInfo  implements java.io.Serializable {

    /*** Required Fields ***/
    private String make;
    private String model;
    private String platform;
    private String OSVersion;
    private String operationType;
    private String imei;
    private String buildNumber;
    private String firmware;
    private long freeStorage;
    private long totalStorage;
    private long startDateTime;
    private long endDateTime;
    private String serialNumber;
    private String pid;
    private String vid;
    private String USBUDIDSerialNo;
    private String USBDriverStatus;
    private Integer timeForDetection;
    private String rootedStatus;
    private Integer attemptsToConnect;
    private EDeviceSwitchContentTransferDetail[] EDeviceSwitchContentTransferDetailCollection;

    private Long deviceInfoId;

    //Not needed fields for now. Adding transient keyword will exclude them from JSON serialization.
    private transient EDeviceSwitchSession EDeviceSwitchSession1;
    private transient EDeviceSwitchSession EDeviceSwitchSession;
    private transient long parentSessionId;
    private transient Integer capacityInGB;
    private transient String brand;
    private transient String BTMACId;
    private transient String basebandSerialNo;
    private transient String basebandVersion;
    private transient String batterySerialNo;
    private transient Integer batteyCycleCount;
    private transient String bootloaderVersion;
    private transient String CSCCode;
    private transient String CSCVersion;
    private transient long callTimerInSec;
    private transient String carrier;
    private transient String carrierLockStatus;
    private transient String color;
    private transient long creationDttm;
    private transient String deviceAddtionalAttributes;
    private transient String dispalySerialNo;
    private transient String ecid;
    private transient String etherMACId;
    private transient String FMiPStatus;
    private transient String frontCamSerialNo;
    private transient String GFRPStatus;
    private transient String iccid;
    private transient String imsi;
    private transient String initialModeOfConnection;
    private transient long lastUpdatedDttm;
    private transient String lockStatus;
    private transient String manufacturedWee;
    private transient String networkType;
    private transient String phoneNumber;
    private transient String RLStatus;
    private transient String rearCamSerialNo;
    private transient Integer SDCardSizeInMB;
    private transient String SDCardStatus;
    private transient String SIMCardStatus;
    private transient Integer timeToGetDeviceInfo;
    private transient String touchPadSerialNo;
    private transient String uniqueDeviceID3;
    private transient String uniqueDeviceID4;
    private transient String uniqueDeviceID5;
    private transient String wifiMACId;
    private transient String wifiSerialNo;

    public EDeviceInfo() {
    }

    public EDeviceInfo(
            Integer attemptsToConnect,
            String BTMACId,
            String basebandSerialNo,
            String basebandVersion,
            String batterySerialNo,
            Integer batteyCycleCount,
            String bootloaderVersion,
            String brand,
            String buildNumber,
            String CSCCode,
            String CSCVersion,
            long callTimerInSec,
            Integer capacityInGB,
            String carrier,
            String carrierLockStatus,
            String color,
            long creationDttm,
            String deviceAddtionalAttributes,
            Long deviceInfoId,
            String dispalySerialNo,
            EDeviceSwitchContentTransferDetail[] EDeviceSwitchContentTransferDetailCollection,
            EDeviceSwitchSession EDeviceSwitchSession,
            EDeviceSwitchSession EDeviceSwitchSession1,
            String ecid,
            long endDateTime,
            String etherMACId,
            String FMiPStatus,
            String firmware,
            long freeStorage,
            String frontCamSerialNo,
            String GFRPStatus,
            String iccid,
            String imei,
            String imsi,
            String initialModeOfConnection,
            long lastUpdatedDttm,
            String lockStatus,
            String make,
            String manufacturedWee,
            String model,
            String networkType,
            String OSVersion,
            String operationType,
            long parentSessionId,
            String phoneNumber,
            String pid,
            String platform,
            String RLStatus,
            String rearCamSerialNo,
            String rootedStatus,
            Integer SDCardSizeInMB,
            String SDCardStatus,
            String SIMCardStatus,
            String serialNumber,
            long startDateTime,
            Integer timeForDetection,
            Integer timeToGetDeviceInfo,
            long totalStorage,
            String touchPadSerialNo,
            String USBDriverStatus,
            String USBUDIDSerialNo,
            String uniqueDeviceID3,
            String uniqueDeviceID4,
            String uniqueDeviceID5,
            String vid,
            String wifiMACId,
            String wifiSerialNo) {
        this.attemptsToConnect = attemptsToConnect;
        this.BTMACId = BTMACId;
        this.basebandSerialNo = basebandSerialNo;
        this.basebandVersion = basebandVersion;
        this.batterySerialNo = batterySerialNo;
        this.batteyCycleCount = batteyCycleCount;
        this.bootloaderVersion = bootloaderVersion;
        this.brand = brand;
        this.buildNumber = buildNumber;
        this.CSCCode = CSCCode;
        this.CSCVersion = CSCVersion;
        this.callTimerInSec = callTimerInSec;
        this.capacityInGB = capacityInGB;
        this.carrier = carrier;
        this.carrierLockStatus = carrierLockStatus;
        this.color = color;
        this.creationDttm = creationDttm;
        this.deviceAddtionalAttributes = deviceAddtionalAttributes;
        this.deviceInfoId = deviceInfoId;
        this.dispalySerialNo = dispalySerialNo;
        this.EDeviceSwitchContentTransferDetailCollection = EDeviceSwitchContentTransferDetailCollection;
        this.EDeviceSwitchSession = EDeviceSwitchSession;
        this.EDeviceSwitchSession1 = EDeviceSwitchSession1;
        this.ecid = ecid;
        this.endDateTime = endDateTime;
        this.etherMACId = etherMACId;
        this.FMiPStatus = FMiPStatus;
        this.firmware = firmware;
        this.freeStorage = freeStorage;
        this.frontCamSerialNo = frontCamSerialNo;
        this.GFRPStatus = GFRPStatus;
        this.iccid = iccid;
        this.imei = imei;
        this.imsi = imsi;
        this.initialModeOfConnection = initialModeOfConnection;
        this.lastUpdatedDttm = lastUpdatedDttm;
        this.lockStatus = lockStatus;
        this.make = make;
        this.manufacturedWee = manufacturedWee;
        this.model = model;
        this.networkType = networkType;
        this.OSVersion = OSVersion;
        this.operationType = operationType;
        this.parentSessionId = parentSessionId;
        this.phoneNumber = phoneNumber;
        this.pid = pid;
        this.platform = platform;
        this.RLStatus = RLStatus;
        this.rearCamSerialNo = rearCamSerialNo;
        this.rootedStatus = rootedStatus;
        this.SDCardSizeInMB = SDCardSizeInMB;
        this.SDCardStatus = SDCardStatus;
        this.SIMCardStatus = SIMCardStatus;
        this.serialNumber = serialNumber;
        this.startDateTime = startDateTime;
        this.timeForDetection = timeForDetection;
        this.timeToGetDeviceInfo = timeToGetDeviceInfo;
        this.totalStorage = totalStorage;
        this.touchPadSerialNo = touchPadSerialNo;
        this.USBDriverStatus = USBDriverStatus;
        this.USBUDIDSerialNo = USBUDIDSerialNo;
        this.uniqueDeviceID3 = uniqueDeviceID3;
        this.uniqueDeviceID4 = uniqueDeviceID4;
        this.uniqueDeviceID5 = uniqueDeviceID5;
        this.vid = vid;
        this.wifiMACId = wifiMACId;
        this.wifiSerialNo = wifiSerialNo;
    }


    /**
     * Gets the attemptsToConnect value for this EDeviceInfo.
     *
     * @return attemptsToConnect
     */
    public Integer getAttemptsToConnect() {
        return attemptsToConnect;
    }


    /**
     * Sets the attemptsToConnect value for this EDeviceInfo.
     *
     * @param attemptsToConnect
     */
    public void setAttemptsToConnect(Integer attemptsToConnect) {
        this.attemptsToConnect = attemptsToConnect;
    }


    /**
     * Gets the BTMACId value for this EDeviceInfo.
     *
     * @return BTMACId
     */
    public String getBTMACId() {
        return BTMACId;
    }


    /**
     * Sets the BTMACId value for this EDeviceInfo.
     *
     * @param BTMACId
     */
    public void setBTMACId(String BTMACId) {
        this.BTMACId = BTMACId;
    }


    /**
     * Gets the basebandSerialNo value for this EDeviceInfo.
     *
     * @return basebandSerialNo
     */
    public String getBasebandSerialNo() {
        return basebandSerialNo;
    }


    /**
     * Sets the basebandSerialNo value for this EDeviceInfo.
     *
     * @param basebandSerialNo
     */
    public void setBasebandSerialNo(String basebandSerialNo) {
        this.basebandSerialNo = basebandSerialNo;
    }


    /**
     * Gets the basebandVersion value for this EDeviceInfo.
     *
     * @return basebandVersion
     */
    public String getBasebandVersion() {
        return basebandVersion;
    }


    /**
     * Sets the basebandVersion value for this EDeviceInfo.
     *
     * @param basebandVersion
     */
    public void setBasebandVersion(String basebandVersion) {
        this.basebandVersion = basebandVersion;
    }


    /**
     * Gets the batterySerialNo value for this EDeviceInfo.
     *
     * @return batterySerialNo
     */
    public String getBatterySerialNo() {
        return batterySerialNo;
    }


    /**
     * Sets the batterySerialNo value for this EDeviceInfo.
     *
     * @param batterySerialNo
     */
    public void setBatterySerialNo(String batterySerialNo) {
        this.batterySerialNo = batterySerialNo;
    }


    /**
     * Gets the batteyCycleCount value for this EDeviceInfo.
     *
     * @return batteyCycleCount
     */
    public Integer getBatteyCycleCount() {
        return batteyCycleCount;
    }


    /**
     * Sets the batteyCycleCount value for this EDeviceInfo.
     *
     * @param batteyCycleCount
     */
    public void setBatteyCycleCount(Integer batteyCycleCount) {
        this.batteyCycleCount = batteyCycleCount;
    }


    /**
     * Gets the bootloaderVersion value for this EDeviceInfo.
     *
     * @return bootloaderVersion
     */
    public String getBootloaderVersion() {
        return bootloaderVersion;
    }


    /**
     * Sets the bootloaderVersion value for this EDeviceInfo.
     *
     * @param bootloaderVersion
     */
    public void setBootloaderVersion(String bootloaderVersion) {
        this.bootloaderVersion = bootloaderVersion;
    }


    /**
     * Gets the brand value for this EDeviceInfo.
     *
     * @return brand
     */

    public String getBrand() {
        return brand;
    }


    /**
     * Sets the brand value for this EDeviceInfo.
     *
     * @param brand
     */
    public void setBrand(String brand) {
        this.brand = brand;
    }


    /**
     * Gets the buildNumber value for this EDeviceInfo.
     *
     * @return buildNumber
     */
    public String getBuildNumber() {
        return buildNumber;
    }


    /**
     * Sets the buildNumber value for this EDeviceInfo.
     *
     * @param buildNumber
     */
    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }


    /**
     * Gets the CSCCode value for this EDeviceInfo.
     *
     * @return CSCCode
     */
    public String getCSCCode() {
        return CSCCode;
    }


    /**
     * Sets the CSCCode value for this EDeviceInfo.
     *
     * @param CSCCode
     */
    public void setCSCCode(String CSCCode) {
        this.CSCCode = CSCCode;
    }


    /**
     * Gets the CSCVersion value for this EDeviceInfo.
     *
     * @return CSCVersion
     */
    public String getCSCVersion() {
        return CSCVersion;
    }


    /**
     * Sets the CSCVersion value for this EDeviceInfo.
     *
     * @param CSCVersion
     */
    public void setCSCVersion(String CSCVersion) {
        this.CSCVersion = CSCVersion;
    }


    /**
     * Gets the callTimerInSec value for this EDeviceInfo.
     *
     * @return callTimerInSec
     */
    public long getCallTimerInSec() {
        return callTimerInSec;
    }


    /**
     * Sets the callTimerInSec value for this EDeviceInfo.
     *
     * @param callTimerInSec
     */
    public void setCallTimerInSec(long callTimerInSec) {
        this.callTimerInSec = callTimerInSec;
    }


    /**
     * Gets the capacityInGB value for this EDeviceInfo.
     *
     * @return capacityInGB
     */
    public Integer getCapacityInGB() {
        return capacityInGB;
    }


    /**
     * Sets the capacityInGB value for this EDeviceInfo.
     *
     * @param capacityInGB
     */
    public void setCapacityInGB(Integer capacityInGB) {
        this.capacityInGB = capacityInGB;
    }


    /**
     * Gets the carrier value for this EDeviceInfo.
     *
     * @return carrier
     */
    public String getCarrier() {
        return carrier;
    }


    /**
     * Sets the carrier value for this EDeviceInfo.
     *
     * @param carrier
     */
    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }


    /**
     * Gets the carrierLockStatus value for this EDeviceInfo.
     *
     * @return carrierLockStatus
     */
    public String getCarrierLockStatus() {
        return carrierLockStatus;
    }


    /**
     * Sets the carrierLockStatus value for this EDeviceInfo.
     *
     * @param carrierLockStatus
     */
    public void setCarrierLockStatus(String carrierLockStatus) {
        this.carrierLockStatus = carrierLockStatus;
    }


    /**
     * Gets the color value for this EDeviceInfo.
     *
     * @return color
     */
    public String getColor() {
        return color;
    }


    /**
     * Sets the color value for this EDeviceInfo.
     *
     * @param color
     */
    public void setColor(String color) {
        this.color = color;
    }


    /**
     * Gets the creationDttm value for this EDeviceInfo.
     *
     * @return creationDttm
     */
    public long getCreationDttm() {
        return creationDttm;
    }


    /**
     * Sets the creationDttm value for this EDeviceInfo.
     *
     * @param creationDttm
     */
    public void setCreationDttm(long creationDttm) {
        this.creationDttm = creationDttm;
    }


    /**
     * Gets the deviceAddtionalAttributes value for this EDeviceInfo.
     *
     * @return deviceAddtionalAttributes
     */
    public String getDeviceAddtionalAttributes() {
        return deviceAddtionalAttributes;
    }


    /**
     * Sets the deviceAddtionalAttributes value for this EDeviceInfo.
     *
     * @param deviceAddtionalAttributes
     */
    public void setDeviceAddtionalAttributes(String deviceAddtionalAttributes) {
        this.deviceAddtionalAttributes = deviceAddtionalAttributes;
    }


    /**
     * Gets the deviceInfoId value for this EDeviceInfo.
     *
     * @return deviceInfoId
     */
    public Long getDeviceInfoId() {
        return deviceInfoId;
    }


    /**
     * Sets the deviceInfoId value for this EDeviceInfo.
     *
     * @param deviceInfoId
     */
    public void setDeviceInfoId(Long deviceInfoId) {
        this.deviceInfoId = deviceInfoId;
    }


    /**
     * Gets the dispalySerialNo value for this EDeviceInfo.
     *
     * @return dispalySerialNo
     */
    public String getDispalySerialNo() {
        return dispalySerialNo;
    }


    /**
     * Sets the dispalySerialNo value for this EDeviceInfo.
     *
     * @param dispalySerialNo
     */
    public void setDispalySerialNo(String dispalySerialNo) {
        this.dispalySerialNo = dispalySerialNo;
    }


    /**
     * Gets the EDeviceSwitchContentTransferDetailCollection value for this EDeviceInfo.
     *
     * @return EDeviceSwitchContentTransferDetailCollection
     */
    public EDeviceSwitchContentTransferDetail[] getEDeviceSwitchContentTransferDetailCollection() {
        return EDeviceSwitchContentTransferDetailCollection;
    }


    /**
     * Sets the EDeviceSwitchContentTransferDetailCollection value for this EDeviceInfo.
     *
     * @param EDeviceSwitchContentTransferDetailCollection
     */
    public void setEDeviceSwitchContentTransferDetailCollection(EDeviceSwitchContentTransferDetail[] EDeviceSwitchContentTransferDetailCollection) {
        this.EDeviceSwitchContentTransferDetailCollection = EDeviceSwitchContentTransferDetailCollection;
    }

    public EDeviceSwitchContentTransferDetail getEDeviceSwitchContentTransferDetailCollection(int i) {
        return this.EDeviceSwitchContentTransferDetailCollection[i];
    }

    public void setEDeviceSwitchContentTransferDetailCollection(int i, EDeviceSwitchContentTransferDetail _value) {
        this.EDeviceSwitchContentTransferDetailCollection[i] = _value;
    }


    /**
     * Gets the EDeviceSwitchSession value for this EDeviceInfo.
     *
     * @return EDeviceSwitchSession
     */
    public EDeviceSwitchSession getEDeviceSwitchSession() {
        return EDeviceSwitchSession;
    }


    /**
     * Sets the EDeviceSwitchSession value for this EDeviceInfo.
     *
     * @param EDeviceSwitchSession
     */
    public void setEDeviceSwitchSession(EDeviceSwitchSession EDeviceSwitchSession) {
        this.EDeviceSwitchSession = EDeviceSwitchSession;
    }


    /**
     * Gets the EDeviceSwitchSession1 value for this EDeviceInfo.
     *
     * @return EDeviceSwitchSession1
     */
    public EDeviceSwitchSession getEDeviceSwitchSession1() {
        return EDeviceSwitchSession1;
    }


    /**
     * Sets the EDeviceSwitchSession1 value for this EDeviceInfo.
     *
     * @param EDeviceSwitchSession1
     */
    public void setEDeviceSwitchSession1(EDeviceSwitchSession EDeviceSwitchSession1) {
        this.EDeviceSwitchSession1 = EDeviceSwitchSession1;
    }


    /**
     * Gets the ecid value for this EDeviceInfo.
     *
     * @return ecid
     */
    public String getEcid() {
        return ecid;
    }


    /**
     * Sets the ecid value for this EDeviceInfo.
     *
     * @param ecid
     */
    public void setEcid(String ecid) {
        this.ecid = ecid;
    }


    /**
     * Gets the endDateTime value for this EDeviceInfo.
     *
     * @return endDateTime
     */
    public long getEndDateTime() {
        return endDateTime;
    }


    /**
     * Sets the endDateTime value for this EDeviceInfo.
     *
     * @param endDateTime
     */
    public void setEndDateTime(long endDateTime) {
        this.endDateTime = endDateTime;
    }


    /**
     * Gets the etherMACId value for this EDeviceInfo.
     *
     * @return etherMACId
     */
    public String getEtherMACId() {
        return etherMACId;
    }


    /**
     * Sets the etherMACId value for this EDeviceInfo.
     *
     * @param etherMACId
     */
    public void setEtherMACId(String etherMACId) {
        this.etherMACId = etherMACId;
    }


    /**
     * Gets the FMiPStatus value for this EDeviceInfo.
     *
     * @return FMiPStatus
     */
    public String getFMiPStatus() {
        return FMiPStatus;
    }


    /**
     * Sets the FMiPStatus value for this EDeviceInfo.
     *
     * @param FMiPStatus
     */
    public void setFMiPStatus(String FMiPStatus) {
        this.FMiPStatus = FMiPStatus;
    }


    /**
     * Gets the firmware value for this EDeviceInfo.
     *
     * @return firmware
     */
    public String getFirmware() {
        return firmware;
    }


    /**
     * Sets the firmware value for this EDeviceInfo.
     *
     * @param firmware
     */
    public void setFirmware(String firmware) {
        this.firmware = firmware;
    }


    /**
     * Gets the freeStorage value for this EDeviceInfo.
     *
     * @return freeStorage
     */
    public long getFreeStorage() {
        return freeStorage;
    }


    /**
     * Sets the freeStorage value for this EDeviceInfo.
     *
     * @param freeStorage
     */
    public void setFreeStorage(long freeStorage) {
        this.freeStorage = freeStorage;
    }


    /**
     * Gets the frontCamSerialNo value for this EDeviceInfo.
     *
     * @return frontCamSerialNo
     */
    public String getFrontCamSerialNo() {
        return frontCamSerialNo;
    }


    /**
     * Sets the frontCamSerialNo value for this EDeviceInfo.
     *
     * @param frontCamSerialNo
     */
    public void setFrontCamSerialNo(String frontCamSerialNo) {
        this.frontCamSerialNo = frontCamSerialNo;
    }


    /**
     * Gets the GFRPStatus value for this EDeviceInfo.
     *
     * @return GFRPStatus
     */
    public String getGFRPStatus() {
        return GFRPStatus;
    }


    /**
     * Sets the GFRPStatus value for this EDeviceInfo.
     *
     * @param GFRPStatus
     */
    public void setGFRPStatus(String GFRPStatus) {
        this.GFRPStatus = GFRPStatus;
    }


    /**
     * Gets the iccid value for this EDeviceInfo.
     *
     * @return iccid
     */
    public String getIccid() {
        return iccid;
    }


    /**
     * Sets the iccid value for this EDeviceInfo.
     *
     * @param iccid
     */
    public void setIccid(String iccid) {
        this.iccid = iccid;
    }


    /**
     * Gets the imei value for this EDeviceInfo.
     *
     * @return imei
     */
    public String getImei() {
        return imei;
    }


    /**
     * Sets the imei value for this EDeviceInfo.
     *
     * @param imei
     */
    public void setImei(String imei) {
        this.imei = imei;
    }


    /**
     * Gets the imsi value for this EDeviceInfo.
     *
     * @return imsi
     */
    public String getImsi() {
        return imsi;
    }


    /**
     * Sets the imsi value for this EDeviceInfo.
     *
     * @param imsi
     */
    public void setImsi(String imsi) {
        this.imsi = imsi;
    }


    /**
     * Gets the initialModeOfConnection value for this EDeviceInfo.
     *
     * @return initialModeOfConnection
     */
    public String getInitialModeOfConnection() {
        return initialModeOfConnection;
    }


    /**
     * Sets the initialModeOfConnection value for this EDeviceInfo.
     *
     * @param initialModeOfConnection
     */
    public void setInitialModeOfConnection(String initialModeOfConnection) {
        this.initialModeOfConnection = initialModeOfConnection;
    }


    /**
     * Gets the lastUpdatedDttm value for this EDeviceInfo.
     *
     * @return lastUpdatedDttm
     */
    public long getLastUpdatedDttm() {
        return lastUpdatedDttm;
    }


    /**
     * Sets the lastUpdatedDttm value for this EDeviceInfo.
     *
     * @param lastUpdatedDttm
     */
    public void setLastUpdatedDttm(long lastUpdatedDttm) {
        this.lastUpdatedDttm = lastUpdatedDttm;
    }


    /**
     * Gets the lockStatus value for this EDeviceInfo.
     *
     * @return lockStatus
     */
    public String getLockStatus() {
        return lockStatus;
    }


    /**
     * Sets the lockStatus value for this EDeviceInfo.
     *
     * @param lockStatus
     */
    public void setLockStatus(String lockStatus) {
        this.lockStatus = lockStatus;
    }


    /**
     * Gets the make value for this EDeviceInfo.
     *
     * @return make
     */
    public String getMake() {
        return make;
    }


    /**
     * Sets the make value for this EDeviceInfo.
     *
     * @param make
     */
    public void setMake(String make) {
        this.make = make;
    }


    /**
     * Gets the manufacturedWee value for this EDeviceInfo.
     *
     * @return manufacturedWee
     */
    public String getManufacturedWee() {
        return manufacturedWee;
    }


    /**
     * Sets the manufacturedWee value for this EDeviceInfo.
     *
     * @param manufacturedWee
     */
    public void setManufacturedWee(String manufacturedWee) {
        this.manufacturedWee = manufacturedWee;
    }


    /**
     * Gets the model value for this EDeviceInfo.
     *
     * @return model
     */
    public String getModel() {
        return model;
    }


    /**
     * Sets the model value for this EDeviceInfo.
     *
     * @param model
     */
    public void setModel(String model) {
        this.model = model;
    }


    /**
     * Gets the networkType value for this EDeviceInfo.
     *
     * @return networkType
     */
    public String getNetworkType() {
        return networkType;
    }


    /**
     * Sets the networkType value for this EDeviceInfo.
     *
     * @param networkType
     */
    public void setNetworkType(String networkType) {
        this.networkType = networkType;
    }


    /**
     * Gets the OSVersion value for this EDeviceInfo.
     *
     * @return OSVersion
     */
    public String getOSVersion() {
        return OSVersion;
    }


    /**
     * Sets the OSVersion value for this EDeviceInfo.
     *
     * @param OSVersion
     */
    public void setOSVersion(String OSVersion) {
        this.OSVersion = OSVersion;
    }


    /**
     * Gets the operationType value for this EDeviceInfo.
     *
     * @return operationType
     */
    public String getOperationType() {
        return operationType;
    }


    /**
     * Sets the operationType value for this EDeviceInfo.
     *
     * @param operationType
     */
    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }


    /**
     * Gets the parentSessionId value for this EDeviceInfo.
     *
     * @return parentSessionId
     */
    public long getParentSessionId() {
        return parentSessionId;
    }


    /**
     * Sets the parentSessionId value for this EDeviceInfo.
     *
     * @param parentSessionId
     */
    public void setParentSessionId(long parentSessionId) {
        this.parentSessionId = parentSessionId;
    }


    /**
     * Gets the phoneNumber value for this EDeviceInfo.
     *
     * @return phoneNumber
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }


    /**
     * Sets the phoneNumber value for this EDeviceInfo.
     *
     * @param phoneNumber
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }


    /**
     * Gets the pid value for this EDeviceInfo.
     *
     * @return pid
     */
    public String getPid() {
        return pid;
    }


    /**
     * Sets the pid value for this EDeviceInfo.
     *
     * @param pid
     */
    public void setPid(String pid) {
        this.pid = pid;
    }


    /**
     * Gets the platform value for this EDeviceInfo.
     *
     * @return platform
     */
    public String getPlatform() {
        return platform;
    }


    /**
     * Sets the platform value for this EDeviceInfo.
     *
     * @param platform
     */
    public void setPlatform(String platform) {
        this.platform = platform;
    }


    /**
     * Gets the RLStatus value for this EDeviceInfo.
     *
     * @return RLStatus
     */
    public String getRLStatus() {
        return RLStatus;
    }


    /**
     * Sets the RLStatus value for this EDeviceInfo.
     *
     * @param RLStatus
     */
    public void setRLStatus(String RLStatus) {
        this.RLStatus = RLStatus;
    }


    /**
     * Gets the rearCamSerialNo value for this EDeviceInfo.
     *
     * @return rearCamSerialNo
     */
    public String getRearCamSerialNo() {
        return rearCamSerialNo;
    }


    /**
     * Sets the rearCamSerialNo value for this EDeviceInfo.
     *
     * @param rearCamSerialNo
     */
    public void setRearCamSerialNo(String rearCamSerialNo) {
        this.rearCamSerialNo = rearCamSerialNo;
    }


    /**
     * Gets the rootedStatus value for this EDeviceInfo.
     *
     * @return rootedStatus
     */
    public String getRootedStatus() {
        return rootedStatus;
    }


    /**
     * Sets the rootedStatus value for this EDeviceInfo.
     *
     * @param rootedStatus
     */
    public void setRootedStatus(String rootedStatus) {
        this.rootedStatus = rootedStatus;
    }


    /**
     * Gets the SDCardSizeInMB value for this EDeviceInfo.
     *
     * @return SDCardSizeInMB
     */
    public Integer getSDCardSizeInMB() {
        return SDCardSizeInMB;
    }


    /**
     * Sets the SDCardSizeInMB value for this EDeviceInfo.
     *
     * @param SDCardSizeInMB
     */
    public void setSDCardSizeInMB(Integer SDCardSizeInMB) {
        this.SDCardSizeInMB = SDCardSizeInMB;
    }


    /**
     * Gets the SDCardStatus value for this EDeviceInfo.
     *
     * @return SDCardStatus
     */
    public String getSDCardStatus() {
        return SDCardStatus;
    }


    /**
     * Sets the SDCardStatus value for this EDeviceInfo.
     *
     * @param SDCardStatus
     */
    public void setSDCardStatus(String SDCardStatus) {
        this.SDCardStatus = SDCardStatus;
    }


    /**
     * Gets the SIMCardStatus value for this EDeviceInfo.
     *
     * @return SIMCardStatus
     */
    public String getSIMCardStatus() {
        return SIMCardStatus;
    }


    /**
     * Sets the SIMCardStatus value for this EDeviceInfo.
     *
     * @param SIMCardStatus
     */
    public void setSIMCardStatus(String SIMCardStatus) {
        this.SIMCardStatus = SIMCardStatus;
    }


    /**
     * Gets the serialNumber value for this EDeviceInfo.
     *
     * @return serialNumber
     */
    public String getSerialNumber() {
        return serialNumber;
    }


    /**
     * Sets the serialNumber value for this EDeviceInfo.
     *
     * @param serialNumber
     */
    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }


    /**
     * Gets the startDateTime value for this EDeviceInfo.
     *
     * @return startDateTime
     */
    public long getStartDateTime() {
        return startDateTime;
    }


    /**
     * Sets the startDateTime value for this EDeviceInfo.
     *
     * @param startDateTime
     */
    public void setStartDateTime(long startDateTime) {
        this.startDateTime = startDateTime;
    }


    /**
     * Gets the timeForDetection value for this EDeviceInfo.
     *
     * @return timeForDetection
     */
    public Integer getTimeForDetection() {
        return timeForDetection;
    }


    /**
     * Sets the timeForDetection value for this EDeviceInfo.
     *
     * @param timeForDetection
     */
    public void setTimeForDetection(Integer timeForDetection) {
        this.timeForDetection = timeForDetection;
    }


    /**
     * Gets the timeToGetDeviceInfo value for this EDeviceInfo.
     *
     * @return timeToGetDeviceInfo
     */
    public Integer getTimeToGetDeviceInfo() {
        return timeToGetDeviceInfo;
    }


    /**
     * Sets the timeToGetDeviceInfo value for this EDeviceInfo.
     *
     * @param timeToGetDeviceInfo
     */
    public void setTimeToGetDeviceInfo(Integer timeToGetDeviceInfo) {
        this.timeToGetDeviceInfo = timeToGetDeviceInfo;
    }


    /**
     * Gets the totalStorage value for this EDeviceInfo.
     *
     * @return totalStorage
     */
    public long getTotalStorage() {
        return totalStorage;
    }


    /**
     * Sets the totalStorage value for this EDeviceInfo.
     *
     * @param totalStorage
     */
    public void setTotalStorage(long totalStorage) {
        this.totalStorage = totalStorage;
    }


    /**
     * Gets the touchPadSerialNo value for this EDeviceInfo.
     *
     * @return touchPadSerialNo
     */
    public String getTouchPadSerialNo() {
        return touchPadSerialNo;
    }


    /**
     * Sets the touchPadSerialNo value for this EDeviceInfo.
     *
     * @param touchPadSerialNo
     */
    public void setTouchPadSerialNo(String touchPadSerialNo) {
        this.touchPadSerialNo = touchPadSerialNo;
    }


    /**
     * Gets the USBDriverStatus value for this EDeviceInfo.
     *
     * @return USBDriverStatus
     */
    public String getUSBDriverStatus() {
        return USBDriverStatus;
    }


    /**
     * Sets the USBDriverStatus value for this EDeviceInfo.
     *
     * @param USBDriverStatus
     */
    public void setUSBDriverStatus(String USBDriverStatus) {
        this.USBDriverStatus = USBDriverStatus;
    }


    /**
     * Gets the USBUDIDSerialNo value for this EDeviceInfo.
     *
     * @return USBUDIDSerialNo
     */
    public String getUSBUDIDSerialNo() {
        return USBUDIDSerialNo;
    }


    /**
     * Sets the USBUDIDSerialNo value for this EDeviceInfo.
     *
     * @param USBUDIDSerialNo
     */
    public void setUSBUDIDSerialNo(String USBUDIDSerialNo) {
        this.USBUDIDSerialNo = USBUDIDSerialNo;
    }


    /**
     * Gets the uniqueDeviceID3 value for this EDeviceInfo.
     *
     * @return uniqueDeviceID3
     */
    public String getUniqueDeviceID3() {
        return uniqueDeviceID3;
    }


    /**
     * Sets the uniqueDeviceID3 value for this EDeviceInfo.
     *
     * @param uniqueDeviceID3
     */
    public void setUniqueDeviceID3(String uniqueDeviceID3) {
        this.uniqueDeviceID3 = uniqueDeviceID3;
    }


    /**
     * Gets the uniqueDeviceID4 value for this EDeviceInfo.
     *
     * @return uniqueDeviceID4
     */
    public String getUniqueDeviceID4() {
        return uniqueDeviceID4;
    }


    /**
     * Sets the uniqueDeviceID4 value for this EDeviceInfo.
     *
     * @param uniqueDeviceID4
     */
    public void setUniqueDeviceID4(String uniqueDeviceID4) {
        this.uniqueDeviceID4 = uniqueDeviceID4;
    }


    /**
     * Gets the uniqueDeviceID5 value for this EDeviceInfo.
     *
     * @return uniqueDeviceID5
     */
    public String getUniqueDeviceID5() {
        return uniqueDeviceID5;
    }


    /**
     * Sets the uniqueDeviceID5 value for this EDeviceInfo.
     *
     * @param uniqueDeviceID5
     */
    public void setUniqueDeviceID5(String uniqueDeviceID5) {
        this.uniqueDeviceID5 = uniqueDeviceID5;
    }


    /**
     * Gets the vid value for this EDeviceInfo.
     *
     * @return vid
     */
    public String getVid() {
        return vid;
    }


    /**
     * Sets the vid value for this EDeviceInfo.
     *
     * @param vid
     */
    public void setVid(String vid) {
        this.vid = vid;
    }


    /**
     * Gets the wifiMACId value for this EDeviceInfo.
     *
     * @return wifiMACId
     */
    public String getWifiMACId() {
        return wifiMACId;
    }


    /**
     * Sets the wifiMACId value for this EDeviceInfo.
     *
     * @param wifiMACId
     */
    public void setWifiMACId(String wifiMACId) {
        this.wifiMACId = wifiMACId;
    }


    /**
     * Gets the wifiSerialNo value for this EDeviceInfo.
     *
     * @return wifiSerialNo
     */
    public String getWifiSerialNo() {
        return wifiSerialNo;
    }


    /**
     * Sets the wifiSerialNo value for this EDeviceInfo.
     *
     * @param wifiSerialNo
     */
    public void setWifiSerialNo(String wifiSerialNo) {
        this.wifiSerialNo = wifiSerialNo;
    }
}

