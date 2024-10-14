//
//  Api.swift
//  smap
//
//  Created by  Corp. Dmonster on 12/15/23.
//

import Foundation
import Alamofire

enum networkError: Error {
    case dataFail
}

class Api {
    static let shared = Api()
    let baseUrl = Http.shared.BASE_URL
    let baseFileApiUrl = Http.shared.FILE_API_URL
    
    func auth(dic: Dictionary<String, Any>, completion: @escaping (BaseResult<AuthData>?, Error?) -> ()){
        let url = self.baseUrl + Http.shared.authUrl
        print("auth - \(url)")
        
        let params = [
            "mt_token_id": dic["mt_token_id"] ?? String()
        ] as Dictionary<String, Any>
        
        self.genericFetchJSONData(urlString: url, params:params, completion: completion)
    }
    
    func memberLocation(dic: Dictionary<String, Any>, completion: @escaping (BaseResult<AuthData>?, Error?) -> ()){
        let url = self.baseUrl + Http.shared.memberLocationUrl
        
        let params = [
            "mt_idx": dic["mt_idx"] ?? String(),
            "mt_gps_data": dic["mt_gps_data"] ?? String(),
            "mlt_battery": dic["mlt_battery"] ?? String(),
            "mlt_fine_location": dic["mlt_fine_location"] ?? String(),
            "mlt_location_chk": dic["mlt_location_chk"] ?? String(),
            "mt_health_work": dic["mt_health_work"] ?? String()
        ] as Dictionary<String, Any>
        
        self.genericFetchJSONData(urlString: url, params:params, completion: completion)
    }
    
    func fileUpload(dic: Dictionary<String, Any>, photoName: String?, photo: Data?, completion: @escaping (BaseResult<AuthData>?, Error?) -> ()) {
        
        let url = self.baseFileApiUrl + Http.shared.fileUploadUrl
        print("url - \(url)")
        let params = [
            "mt_idx": dic["mt_idx"] ?? String()
        ] as Dictionary<String, Any>
        
        genericFetchJSONData(urlString: url, params:params, photoName: photoName, photo: photo, completion: completion)
    }
    
    //인앱 결제 결과
    func setOrder() {
        
    
    }
    
    func setRestore() {
        
    }
    
    func setCheck() {
        
    }
    
    //일반 Api 통신
    func genericFetchJSONData<T:Decodable>(urlString:String, params:Dictionary<String, Any>, completion: @escaping (T?, Error?)->Void){
        print(">>>genericFetchJSONData")
        print("urlString - \(urlString) \(params)")
        
        AF.request(urlString, method: .post, parameters: params, encoding: URLEncoding.default)
            .validate(statusCode: 200 ..< 300).response { AFdata in
                
                do {
                    guard let AFdataData = AFdata.data else {
                        print("Error: AFdata data Error")
                        completion(nil, networkError.dataFail)
                        return
                    }
                    
                    guard let jsonObject = try JSONSerialization.jsonObject(with: AFdataData) as? [String: Any] else {
                        print("Error: Cannot convert data to JSON object")
                        return
                    }
                    
                    print(">>>jsonObject: \(jsonObject)")
                    
                    guard let prettyJsonData = try? JSONSerialization.data(withJSONObject: jsonObject, options: .prettyPrinted) else {
                        print("Error: Cannot convert JSON object to Pretty JSON data")
                        return
                    }
                    guard let prettyPrintedJson = String(data: prettyJsonData, encoding: .utf8) else {
                        print("Error: Could print JSON in String")
                        return
                    }
                        
                    let socialApps = try JSONDecoder().decode(T.self, from: AFdataData)
                    //print(">>>socialApps: \(socialApps)")
                    
                    completion(socialApps, nil)
                    
                } catch let jsonError as NSError {
                    print("JSON decode failed: \(jsonError)")
                    completion(nil, networkError.dataFail)
                    return
                }
            }
    }
    
    //멀티파트 이미지 업로드
    func genericFetchJSONData<T:Decodable>(urlString:String, params:Dictionary<String, Any>, photoName: String?, photo: Data?, completion: @escaping (T?, Error?)->Void){
        print(">>>genericFetchJSONData")

        let headers: HTTPHeaders = ["Content-type": "multipart/form-data"]//HTTP 헤더
        
        AF.upload(multipartFormData: { (multipart) in
            if let photo = photo, let photoName = photoName {
                let fileName = Utils.shared.randomString(length: 20)
                multipart.append(photo, withName: photoName, fileName: "\(fileName).jpg", mimeType: "image/jpeg")
            }
            
            for (key, value) in params {
                multipart.append("\(value)".data(using: .utf8, allowLossyConversion: false)!, withName: "\(key)")
                //이미지 데이터 외에 같이 전달할 데이터 (여기서는 user, emoji, date, content 등)
            }
        }, to: urlString
        , method: .post
        , headers: headers)
        .validate(statusCode: 200 ..< 300).response { AFdata in
            do {
                guard let AFdataData = AFdata.data else {
                    print("Error: AFdata data Error")
                    completion(nil, networkError.dataFail)
                    return
                }

                guard let jsonObject = try JSONSerialization.jsonObject(with: AFdataData) as? [String: Any] else {
                    print("Error: Cannot convert data to JSON object")
                    return
                }

                print(">>>jsonObject: \(jsonObject)")

                guard let prettyJsonData = try? JSONSerialization.data(withJSONObject: jsonObject, options: .prettyPrinted) else {
                    print("Error: Cannot convert JSON object to Pretty JSON data")
                    return
                }
                guard let prettyPrintedJson = String(data: prettyJsonData, encoding: .utf8) else {
                    print("Error: Could print JSON in String")
                    return
                }

                let socialApps = try JSONDecoder().decode(T.self, from: AFdataData)
                print(">>>socialApps: \(socialApps)")

                completion(socialApps, nil)

            } catch let jsonError as NSError {
                print("JSON decode failed: \(jsonError)")
                completion(nil, networkError.dataFail)
                return
            }
        }
    }
}
