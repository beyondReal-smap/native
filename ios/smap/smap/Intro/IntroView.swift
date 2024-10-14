//
//  IntroView.swift
//  smap
//
//  Created by  Corp. Dmonster on 12/15/23.
//

import UIKit
import AVFoundation
import Photos

class IntroView: UIViewController {
    override func viewDidLoad() {
        super.viewDidLoad()
    
        self.requestNotificationPermission()
    }
    
    private func requestNotificationPermission(){
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert,.sound,.badge], completionHandler: {didAllow,Error in
            if didAllow {
                print("Push: 권한 허용")
            } else {
                print("Push: 권한 거부")
            }
            
            self.requestCameraPermission()
        })
    }
    
    private func requestCameraPermission(){
        AVCaptureDevice.requestAccess(for: .video, completionHandler: { (granted: Bool) in
            if granted {
                print("Camera: 권한 허용")
            } else {
                print("Camera: 권한 거부")
            }
            
            self.requestPhotoLibraryPermission()
        })
    }
    
    private func requestPhotoLibraryPermission() {
        PHPhotoLibrary.requestAuthorization( { status in
            switch status{
            case .authorized:
                print("Album: 권한 허용")
            case .restricted, .notDetermined, .denied:
                print("Album: 선택하지 않음 또는 권한 거부")
            default:
                break
            }
            
            self.appStart()
        })
    }
    
    private func appStart() {
        DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 2) {
            let storyboard = UIStoryboard(name: "Main", bundle: nil)
            let destinationViewController = storyboard.instantiateViewController(withIdentifier: "MainView") as! UINavigationController
            
            let ad = UIApplication.shared.delegate as! AppDelegate
            ad.window?.rootViewController = destinationViewController
        }
    }
}
