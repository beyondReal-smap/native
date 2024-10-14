//
//  LocationService.swift
//  smap
//
//  Created by  Corp. Dmonster on 12/15/23.
//

import UIKit
import CoreLocation
import CoreMotion

public class LocationService: NSObject, CLLocationManagerDelegate{
    public static var sharedInstance = LocationService()
    let locationManager: CLLocationManager
    
    let userNotiCenter = UNUserNotificationCenter.current()
    
    let activityManager = CMMotionActivityManager()
    let pedoMeter = CMPedometer()
    
    var savedMtIdx = ""
    
    var lastLocation: CLLocation = CLLocation(latitude: 0.0, longitude: 0.0)
    var locationAuthStatus: CLAuthorizationStatus?
    
    override public init() {
        self.locationManager = CLLocationManager()
            
        self.locationManager.desiredAccuracy = kCLLocationAccuracyNearestTenMeters
        self.locationManager.distanceFilter = 10
        
        //locationManager.requestAlwaysAuthorization()
        self.locationManager.requestWhenInUseAuthorization()
        self.locationManager.allowsBackgroundLocationUpdates = true
        self.locationManager.pausesLocationUpdatesAutomatically = false
        self.locationManager.showsBackgroundLocationIndicator = true
        self.locationManager.startMonitoringSignificantLocationChanges()
        
        super.init()
        self.locationManager.delegate = self
        
        NotificationCenter.default.addObserver(self, selector: #selector(self.appStateChange(_:)), name: NSNotification.Name(rawValue: "appStateChange"), object: nil)
    }
    
    public func startUpdatingLocation(){
        self.locationManager.startUpdatingLocation()
    }
    
    public func getLastLocation() -> CLLocation {
        return self.lastLocation
    }
    
    public func auth() {
        Utils.shared.getToken { mt_token_id in
            var dic = Dictionary<String, Any>()
            dic["mt_token_id"] = mt_token_id
            
            Api.shared.auth(dic: dic) { response, error in
                if let error = error {
                    print("can not fetch data", error)
                    return
                }
                
                if let response = response {
                    if response.success == "true" {
                        guard let authData = response.data else { return }
                        self.receiveAuth(authData: authData)
                    } else {
                        print("auth fail - \(response.message ?? "")")
                    }
                } else {
                    print("Network Error")
                }
            }
        }
    }
    
    private func receiveAuth(authData: AuthData) {
        guard let mt_idx = authData.mt_idx else { return }
        self.savedMtIdx = String(mt_idx)
        Utils.shared.setMtIdx(mtIdx: String(mt_idx))
        if self.lastLocation.coordinate.latitude != 0.0 && self.lastLocation.coordinate.longitude != 0.0 {
            let dateFormatter = DateFormatter()
            dateFormatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
            
            let dateTime = dateFormatter.string(from: self.lastLocation.timestamp)
            
            var mltGpsDataList: [MltGpsData] = []
            
//            print("origin lat =============== \(self.lastLocation.coordinate.latitude)")
//            print("origin lat =============== \(self.lastLocation.coordinate.longitude)")
//            print("convert lat =============== \(String(format: "%.5f", self.lastLocation.coordinate.latitude))")
//            print("convert long =============== \(String(format: "%.5f", self.lastLocation.coordinate.longitude))")
            
            var speed = self.lastLocation.speed
            if self.lastLocation.speed < 0 {
                speed = 0
            }
            
            let mlt_lat = String(format: "%.5f", self.lastLocation.coordinate.latitude)
            let mlt_long = String(format: "%.5f", self.lastLocation.coordinate.longitude)
            
//            let mltGpsData = MltGpsData(
//                mlt_lat: String(self.lastLocation.coordinate.latitude),
//                mlt_long: String(self.lastLocation.coordinate.longitude),
//                mlt_speed: String(self.lastLocation.speed),
//                mlt_accuacy: String(self.lastLocation.horizontalAccuracy),
//                mlt_gps_time: dateTime
//            )
            let mltGpsData = MltGpsData(
                mlt_lat: mlt_lat,
                mlt_long: mlt_long,
                mlt_speed: String(speed),
                mlt_accuacy: String(self.lastLocation.horizontalAccuracy),
                mlt_gps_time: dateTime
            )
            
            mltGpsDataList.append(mltGpsData)
            
            self.memberLocation(mltGpsDataList: mltGpsDataList)
        }
    }
    
    private func memberLocation(mltGpsDataList: [MltGpsData]){
        var mt_idx = Utils.shared.getMtIdx()
        if mt_idx == "" {
            mt_idx = self.savedMtIdx
        }
        print("mt_idx - \(mt_idx)")
        if mt_idx != "" && mt_idx != "null" {
            var mltGpsData = Dictionary<String, [MltGpsData]>()
            mltGpsData["mlt_gps_data"] = mltGpsDataList
            
            guard let mltGpsDataJson = try? JSONEncoder().encode(mltGpsData) else {
                return
            }
            
            guard let mltGpsDataJsonString = String(data: mltGpsDataJson, encoding: .utf8) else {
                return
            }
            
            print("mltGpsDataJsonString \(mltGpsDataJsonString)")
            
            // 배터리 정보
            UIDevice.current.isBatteryMonitoringEnabled = true
            let batteryRemain = UIDevice.current.batteryLevel
            let batteryPercent = String(Int(batteryRemain * 100))
            
            self.getStepCount { stepCount in
                var dic = Dictionary<String, Any>()
                
                dic["mt_idx"] = mt_idx
                dic["mt_gps_data"] = mltGpsDataJsonString
                dic["mlt_battery"] = batteryPercent
                dic["mlt_fine_location"] = "N"
                dic["mlt_location_chk"] = "N"
                dic["mt_health_work"] = String(stepCount)
                
                Api.shared.memberLocation(dic: dic) { response, error in
                    if let error = error {
                        print("can not fetch data", error)
                        return
                    }
                    
                    print("memberLocation response - \(response?.success ?? "")")
                }
            }
        
        }
    }
    
    private func getStepCount(completionHandler: @escaping(Int) -> Void) {
        // 걸음정보
        let now = Date()
        let startDate = Calendar.current.startOfDay(for: now)
        
        if CMPedometer.isStepCountingAvailable() {
            self.pedoMeter.queryPedometerData(from: startDate, to: now) { (data, error) in
                var stepData: Int = 0
                if error == nil {
                    if let response = data {
                        stepData = Int(truncating: response.numberOfSteps)
                    }
                }
                
                completionHandler(stepData)
            }
        } else {
            completionHandler(0)
        }
    }
    
    @objc func appStateChange(_ notification: Notification){
        print("LocationService appStateChange")
        let state = notification.userInfo?["state"] as? String
        if state == "foreground" {
            self.locationManager.stopUpdatingLocation()
            self.locationManager.startUpdatingLocation()
        }
    }
    
    // MARK: - CLLocationManagerDelegate
    public func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        print("locations ====> \(locations)")
        if locations.count > 0 {
            
            let dateFormatter = DateFormatter()
            dateFormatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
            
            var mltGpsDataList: [MltGpsData] = []
            
            locations.forEach { location in
                var speed = location.speed
                if location.speed < 0 {
                    speed = 0
                }
                
                let mlt_lat = String(format: "%.5f", self.lastLocation.coordinate.latitude)
                let mlt_long = String(format: "%.5f", self.lastLocation.coordinate.longitude)
                
                let dateTime = dateFormatter.string(from: location.timestamp)
                
                let mltGpsData = MltGpsData(
                    mlt_lat: mlt_lat,
                    mlt_long: mlt_long,
                    mlt_speed: String(speed),
                    mlt_accuacy: String(location.horizontalAccuracy),
                    mlt_gps_time: dateTime
                )
                
                mltGpsDataList.append(mltGpsData)
                
                self.lastLocation = location
            }
            
            if mltGpsDataList.count > 0 {
                self.memberLocation(mltGpsDataList: mltGpsDataList)
            }
        }
    }
    
    public func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        //print("locationManagerDidChangeAuthorization")
        var locationAuthorizationStatus: CLAuthorizationStatus
        if #available(iOS 14.0, *){
            locationAuthorizationStatus = manager.authorizationStatus
        } else {
            locationAuthorizationStatus = CLLocationManager.authorizationStatus()
        }
        
        self.locationAuthStatus = locationAuthorizationStatus

        print("locationAuthorizationStatus \(locationAuthorizationStatus)")
        switch locationAuthorizationStatus {
        case .authorizedAlways, .authorizedWhenInUse:
            print("GPS 권한 설정됨!!!!!!!!!")
            self.locationManager.requestAlwaysAuthorization()
            //self.userNotiCenter.removeAllPendingNotificationRequests()
        case .restricted, .notDetermined:
            print("GPS 권한 설정되지 않음!!!!!!!!!")
        case .denied:
            print("GPS 권한 요청 거부됨!!!!!!!!!")
            //self.requestGpsSendNoti()
        default:
            print("GPS: Default!!!!!!!!!")
        }
    }
}
