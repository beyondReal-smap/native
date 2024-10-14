//
//  Http.swift
//  smap
//
//  Created by  Corp. Dmonster on 3/20/24.
//

import Foundation

class Http {
    static let shared = Http()
    
//    let WEB_BASE_URL = "https://app.smap.site/"
//    let BASE_URL = "https://smap.api.dmonster.kr/api/"//api url
//    let FILE_API_URL = "https://app.smap.site/api/"
    
    let WEB_BASE_URL = "https://app2.smap.site/"
    let BASE_URL = "https://api2.smap.site/api/"//api url
    let FILE_API_URL = "https://app2.smap.site/api/"
    
    let authUrl = "auth/"
    
    let memberLocationUrl = "member_location_json/"
    
    let fileUploadUrl = "member_file_upload.php"
    
    let hashKey = "518cbe9ed50bf7e72913eb6b5a5e5fc6a8b99d56200ebda3a5bb365dbdccbdf6"
}
