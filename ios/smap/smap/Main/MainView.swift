//
//  MainView.swift
//  smap
//
//  Created by  Corp. Dmonster on 12/15/23.
//

import UIKit
import WebKit
import CoreLocation
import MessageUI
import YPImagePicker
import SwiftyStoreKit
import GoogleMobileAds

class MainView: UIViewController {
    var popoverController: UIPopoverPresentationController?// 태블릿용 공유하기 띄우기
    
    private var eventUrl = ""
    private var invitationCode = ""
    
    @IBOutlet weak var loadingView: UIView!
    @IBOutlet weak var indi: UIActivityIndicatorView!
    
    @IBOutlet weak var web_view: WKWebView!
    
    private var webViewPageType = ""
    private var fileUploadMtIdx = ""
    
    private var interstitial: GADInterstitialAd?
    private let interstitialID = "ca-app-pub-7432142706137657/9785898551"
    //private let interstitialID = "ca-app-pub-3940256099942544/4411468910" // - 테스트용
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        self.loadingView.alpha = 0
        self.indi.startAnimating()
        
        NotificationCenter.default.addObserver(self, selector: #selector(self.getPush(_:)), name: NSNotification.Name(rawValue: "getPush"), object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(self.getDeepLink(_:)), name: NSNotification.Name(rawValue: "getDeepLink"), object: nil)
        
        if let invitation_code = UserDefaults.standard.string(forKey: "invitation_code") {
            if !invitation_code.isEmpty {
                if invitation_code != "null" {
                    self.invitationCode = invitation_code
                    UserDefaults.standard.removeObject(forKey: "invitation_code")
                }
            }
        }
        
        if let event_url = UserDefaults.standard.string(forKey: "event_url") {
            if !event_url.isEmpty {
                if event_url != "null" {
                    self.eventUrl = event_url
                    UserDefaults.standard.removeObject(forKey: "event_url")
                }
            }
        }
        
        self.web_view.configuration.userContentController.add(self, name: "smapIos")
        self.web_view.configuration.preferences.javaScriptEnabled = true
        self.web_view.configuration.preferences.javaScriptCanOpenWindowsAutomatically = true
        
        if #available(iOS 14.0, *) {
            self.web_view.configuration.defaultWebpagePreferences.allowsContentJavaScript = true
        } else {
            self.web_view.configuration.preferences.javaScriptEnabled = true
        }
        
        self.web_view.navigationDelegate = self
        self.web_view.uiDelegate = self
        self.web_view.allowsBackForwardNavigationGestures = false
        self.web_view.setKeyboardRequiresUserInteraction(false)
    
        WKWebsiteDataStore.default().fetchDataRecords(ofTypes: WKWebsiteDataStore.allWebsiteDataTypes(), completionHandler: {
            records -> Void in
            records.forEach { WKWebsiteDataStore.default().removeData(ofTypes: $0.dataTypes, for: [$0], completionHandler: {}) }
        })
    
        let location = LocationService.sharedInstance.getLastLocation()
        var urlString = Http.shared.WEB_BASE_URL + "auth?mt_token_id=%@"
    
        if location.coordinate.latitude != 0.0 && location.coordinate.longitude != 0.0 {
            urlString = "\(urlString)&mt_lat=\(location.coordinate.latitude)&mt_long=\(location.coordinate.longitude)"
        }
        
        Utils.shared.getToken { token in
            urlString = String.init(format: urlString, token)
            //print("url String == \(urlString)")
            
            if !self.eventUrl.isEmpty {
                if let eventUrlResult = self.eventUrl.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) {
                   urlString += "&event_url=\(eventUrlResult)"
                }
                self.eventUrl = ""
            }
            
            if !self.invitationCode.isEmpty {
                urlString += "&event_url=\(self.invitationCode)"
                self.invitationCode = ""
            }
            
            let url = URL(string: urlString)
            var request = URLRequest(url: url!)
            request.setValue(Http.shared.hashKey, forHTTPHeaderField: "AUTH_SECRETKEY")
            //print("request - \(request)")
            self.web_view.load(request)
        }
        
        //백그라운드 포그라운드 상태 받기
        NotificationCenter.default.addObserver(self, selector: #selector(self.appStateForeground), name: NSNotification.Name(rawValue: "appStateForeground"), object: nil)
        
        Task {
            await self.loadAds(isShow:false, errorCount:0)
        }
        
        self.locationPermissionCheck()
    }
    
    override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator) {
        super.viewWillTransition(to: size, with: coordinator)
        if let popoverController = self.popoverController {
            popoverController.sourceView = self.view
            popoverController.sourceRect = CGRect(x: size.width*0.5, y: size.height*0.5, width: 0, height: 0)
            popoverController.permittedArrowDirections = []
        }
    }
    
    @objc func appStateForeground(){
        self.web_view.reload()
        
        self.locationPermissionCheck()
        
        let location = LocationService.sharedInstance.getLastLocation()
        let lat = location.coordinate.latitude
        let long = location.coordinate.longitude
        
        //print("location_refresh('\(self.webViewPageType)', '\(lat)', '\(long)');")
        
        self.web_view.evaluateJavaScript("location_refresh('\(self.webViewPageType)', '\(lat)', '\(long)');") { (any, err) -> Void in
            print(err ?? "[location_refresh] IOS >> 자바스크립트 : SUCCESS")
        }
    }
    
    @objc private func getPush(_ notification: Notification) {
        guard let event_url = notification.userInfo?["event_url"] as? String else {return}
        
        if !event_url.isEmpty {
            if event_url != "null" {
                UserDefaults.standard.removeObject(forKey: "event_url")
                let url = URL(string: event_url)
                self.web_view.load(URLRequest(url: url!))
            }
        }
    }
    
    @objc private func getDeepLink(_ notification: Notification) {
        guard let invitation_code = notification.userInfo?["invitation_code"] as? String else { return }
        if !invitation_code.isEmpty {
            if invitation_code != "null" {
                UserDefaults.standard.removeObject(forKey: "invitation_code")
                self.web_view.evaluateJavaScript("invite_code_insert('\(invitation_code)');") { (any, err) -> Void in
                    print(err ?? "[invite_code_insert] IOS >> 자바스크립트 : SUCCESS")
                }
            }
        }
    }
    
    private func showAd() {
        if self.interstitial != nil {
            self.interstitial?.present(fromRootViewController: self)
        } else {
            Task {
                await self.loadAds(isShow: true, errorCount: 1)
            }
        }
    }
    
    private func loadAds(isShow: Bool, errorCount: Int) async {
        self.showLoading()
        do {
            self.interstitial = try await GADInterstitialAd.load(withAdUnitID: self.interstitialID, request: GADRequest())
            self.interstitial?.fullScreenContentDelegate = self
            
            self.hideLoading()
            if isShow {
                DispatchQueue.main.async {
                    self.showAd()
                }
            }
        } catch {
            self.hideLoading()
            print("Failed to load interstitial ad with error: \(error.localizedDescription)")
            if isShow {
                if errorCount == 1 {
                    await self.loadAds(isShow: true, errorCount: errorCount + 1)
                } else if errorCount > 1 {
                    DispatchQueue.main.async {
                        self.web_view.evaluateJavaScript("failAd('load');") { (any, err) -> Void in
                            print(err ?? "[failAd] IOS >> 자바스크립트 : SUCCESS")
                        }
                    }
                }
            }
        }
    }
    
    private func locationPermissionCheck() {
        if LocationService.sharedInstance.locationAuthStatus != .authorizedAlways {
            let mt_idx = Utils.shared.getMtIdx()
            if !mt_idx.isEmpty {

                let title = NSLocalizedString("LOCATION_PERMISSION_TITLE", comment: "")
                let message = NSLocalizedString("LOCATION_PERMISSION_MESSAGE", comment: "")

                let alert = UIAlertController(title: title,
                                            message: message, preferredStyle: .alert)
                let confirm = UIAlertAction(title: NSLocalizedString("LOCATION_PERMISSION_SETTINGS_BUTTON", comment: ""), style: .default) { action in
                    guard let url = URL(string: UIApplication.openSettingsURLString) else { return }

                    if UIApplication.shared.canOpenURL(url) {
                        UIApplication.shared.open(url)
                    }
                }
                let cancel = UIAlertAction(title: NSLocalizedString("LOCATION_PERMISSION_CANCEL_BUTTON", comment: ""), style: .cancel)
                
                alert.addAction(confirm)
                alert.addAction(cancel)
                
                present(alert, animated: true)
            }
        }
    }
    
    private func openPhoto(isCamera: Bool) {
        var config = YPImagePickerConfiguration()
        
        config.library.mediaType = .photo
        config.library.defaultMultipleSelection = false
        config.library.maxNumberOfItems = 1

        if isCamera {
            config.screens = [.photo]
            config.startOnScreen = .photo
        } else {
            config.screens = [.library]
            config.startOnScreen = .library
        }
        
        // cropping style 을 square or not 으로 지정.
        config.library.isSquareByDefault = false

        // 필터 단계 스킵.
        config.showsPhotoFilters = false

        // crop overlay 의 default 색상.
        config.colors.cropOverlayColor = .gray.withAlphaComponent(0.8)
        // 327 * 540 비율로 crop 희망.
        config.showsCrop = .rectangle(ratio: 0.6)

        // 이전에 선택한 이미지가 pre-selected 되도록 함.
        //config.library.preselectedItems = selectedImage

        // 새 이미지를 사진 라이브러리에 저장하지 않음.
        // 👉 저장하지 않으면 selectedImage 에 담긴 이미지가 사진 라이브러리에서 찾을 수가 없어서 가장 앞에 이미지를 선택함.
        config.shouldSaveNewPicturesToAlbum = false

        let imagePicker = YPImagePicker(configuration: config)
        imagePicker.imagePickerDelegate = self

        //imagePicker.didFinishPicking(completion: YPImagePicker.DidFinishPickingCompletion)
        //public typealias DidFinishPickingCompletion = (_ items: [YPMediaItem], _ cancelled: Bool) -> Void
        imagePicker.didFinishPicking { [weak self] items, cancelled in
            guard let self = self else { return }

            if cancelled {
                imagePicker.dismiss(animated: true)
                return
            }

            //selectedImage = items
            if let photo = items.singlePhoto {
                //backgroundImage = photo.image - 이미지 가져오기
                self.fileUpload(image: photo.image)
            }
            imagePicker.dismiss(animated: true)
        }

        imagePicker.modalPresentationStyle = .overFullScreen
        present(imagePicker, animated: true, completion: nil)
    }
    
    private func fileUpload(image: UIImage){
        if let photoData = image.jpegData(compressionQuality: 1.0) {
            var dic = Dictionary<String, Any>()
            dic["mt_idx"] = self.fileUploadMtIdx
            
            UIView.animate(withDuration: 1.0) {
                self.loadingView.alpha = 1
            }
            
            Api.shared.fileUpload(dic: dic, photoName: "mt_file1", photo: photoData) { response, error in
                
                self.loadingView.alpha = 0
                
                if let error = error {
                    print("can not fetch data", error)
                    return
                }
                
                if let response = response {
                    if response.success == "true" {
                        self.web_view.evaluateJavaScript("f_member_file_upload_done();") { (any, err) -> Void in
                            print(err ?? "[f_member_file_upload_done] IOS >> 자바스크립트 : SUCCESS")
                        }
                    } else {
                        Utils.shared.showSnackBar(view: self.view, message: response.message ?? "")
                    }
                } else {
                    Utils.shared.showSnackBar(view: self.view, message: "Network Error")
                }
            }
        } else {
            Utils.shared.showSnackBar(view: self.view, message: "Error - 이미지 정보를 가져오는데 실패했습니다.")
        }
    }
    
    private func urlClipBoard(url: String){
        UIPasteboard.general.string = url
        Utils.shared.showSnackBar(view: self.view, message: "클립보드에 복사 되었습니다.")
    }
    
    private func urlOpenSms(url: String) {
        DispatchQueue.main.async {
            let controller = MFMessageComposeViewController()
            controller.body = url
            controller.messageComposeDelegate = self
            self.present(controller, animated: true, completion: nil)
        }
    }
    
    private func openUrlBlank(url: String) {
        if let _url = URL(string: url) {
            UIApplication.shared.open(_url, options: [:])
        }
    }
    
    private func showLoading() {
        DispatchQueue.main.async {
            self.loadingView.alpha = 1
        }
    }
    
    private func hideLoading() {
        DispatchQueue.main.async {
            self.loadingView.alpha = 0
        }
    }
}


extension MainView: WKNavigationDelegate, WKUIDelegate, WKScriptMessageHandler {
    
    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Swift.Void) {
        
        let address = navigationAction.request.url?.absoluteString
        //print("address :",address ?? String())
        
        if address == "https://ssl.pstatic.net/static/maps/mantle/notice/legal.html" {
            decisionHandler(.cancel)
            return
        } else {
            if let url = navigationAction.request.url {
                if url.scheme == "mailto" || url.scheme == "tel" || url.scheme == "sms" {
                    if UIApplication.shared.canOpenURL(url) {
                        UIApplication.shared.open(url, options: [:], completionHandler: nil)
                    }
                    decisionHandler(.cancel)
                    return
                }
                
                
            }
            
            // 카카오 SDK가 호출하는 커스텀 URL 스킴인 경우 open(_ url:) 메서드를 호출합니다.
            if let url = navigationAction.request.url , ["kakaolink"].contains(url.scheme) {

                UIApplication.shared.open(url, options: [:], completionHandler: nil)

                decisionHandler(.cancel) 
                return
            }
        }
        
        decisionHandler(.allow)
    }
    
    func webView(_ webView: WKWebView, createWebViewWith configuration: WKWebViewConfiguration, for navigationAction: WKNavigationAction, windowFeatures: WKWindowFeatures) -> WKWebView? {
        
        print("createWebViewWith")
        let userContriller = WKUserContentController()
        let script = "var originalWindowClose=window.close;window.close=function(){var iframe=document.createElement('IFRAME');iframe.setAttribute('src','back://'),document.documentElement.appendChild(iframe);originalWindowClose.call(window)};"
        
        let userScript = WKUserScript.init(source: script, injectionTime: .atDocumentStart, forMainFrameOnly: false)
        
        userContriller.addUserScript(userScript)
        
        configuration.preferences.javaScriptCanOpenWindowsAutomatically = true
        configuration.userContentController = userContriller
                
        let newWebView = WKWebView.init(frame: self.web_view.frame, configuration: configuration)
        
        newWebView.navigationDelegate = self
        newWebView.uiDelegate = self
        
        self.view.addSubview(newWebView)
        
        return newWebView
    }
    
    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        print("didFinish")
    }
    
    func webView(_ webView: WKWebView, runJavaScriptAlertPanelWithMessage message: String, initiatedByFrame frame: WKFrameInfo,
                 completionHandler: @escaping () -> Void) {
        
        print("message : ",message)
        
        let alertController = UIAlertController(title: "", message: message, preferredStyle: .alert)
        alertController.addAction(UIAlertAction(title: "확인", style: .default, handler: { (action) in
            completionHandler()
        }))
        
        self.present(alertController, animated: true, completion: nil)
    }
    
    func webView(_ webView: WKWebView, runJavaScriptConfirmPanelWithMessage message: String, initiatedByFrame frame: WKFrameInfo,
                 completionHandler: @escaping (Bool) -> Void) {
        
        print("message : ",message)
        
        let alertController = UIAlertController(title: "", message: message, preferredStyle: .alert)
        alertController.addAction(UIAlertAction(title: "확인", style: .default, handler: { (action) in
            completionHandler(true)
        }))
        alertController.addAction(UIAlertAction(title: "취소", style: .default, handler: { (action) in
            completionHandler(false)
        }))
        
        self.present(alertController, animated: true, completion: nil)
    }
    
    func webView(_ webView: WKWebView, runJavaScriptTextInputPanelWithPrompt prompt: String, defaultText: String?, initiatedByFrame frame: WKFrameInfo,
                 completionHandler: @escaping (String?) -> Void) {
        let alertController = UIAlertController(title: "", message: prompt, preferredStyle: .alert)
        alertController.addTextField { (textField) in
            textField.text = defaultText
        }
        alertController.addAction(UIAlertAction(title: "확인", style: .default, handler: { (action) in
            if let text = alertController.textFields?.first?.text {
                completionHandler(text)
            } else {
                completionHandler(defaultText)
            }
        }))
        
        alertController.addAction(UIAlertAction(title: "취소", style: .default, handler: { (action) in
            completionHandler(nil)
        }))
        
        self.present(alertController, animated: true, completion: nil)
    }
    
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        print("message \(message.name)")
        print("body \(message.body)")
        
        if message.name == "smapIos" {
            guard let body = message.body as? [String: Any] else { return }
            guard let type = body["type"] as? String else { return }
            
            switch type {
            case "pageType":
                guard let page = body["param"] as? String else { return }
                self.webViewPageType = page
                break
                
            case "memberLogin":
                LocationService.sharedInstance.auth();
                break
                
            case "memberLogout":
                LocationService.sharedInstance.savedMtIdx = ""
                Utils.shared.removeMtIdx()
                break
            
            case "openCamera":
                guard let mt_idx = body["param"] as? String else { return }
                self.fileUploadMtIdx = mt_idx
                self.openPhoto(isCamera: true)
                break
                
            case "openAlbum":
                guard let mt_idx = body["param"] as? String else { return }
                self.fileUploadMtIdx = mt_idx
                self.openPhoto(isCamera: false)
                break
            
            case "urlClipBoard":
                guard let url = body["param"] as? String else { return }
                self.urlClipBoard(url: url)
                break
                
            case "urlOpenSms":
                guard let url = body["param"] as? String else { return }
                self.urlOpenSms(url: url)
                break
                
            case "openShare":
                guard let content = body["param"] as? String else { return }
                DispatchQueue.main.async {
                    var objectToShare = [String]()
                    
                    objectToShare.append(content)
                   
                    let activityVC = UIActivityViewController(activityItems : objectToShare, applicationActivities: nil)
                    activityVC.popoverPresentationController?.sourceView = self.view
                    activityVC.popoverPresentationController?.sourceRect = CGRect(x: self.view.bounds.midX, y: self.view.bounds.midY, width: 0, height: 0)
                    activityVC.popoverPresentationController?.permittedArrowDirections = []
                    
                    activityVC.popoverPresentationController?.sourceView = self.view
                    self.present(activityVC, animated: true, completion: nil)
                    if let popoverController = activityVC.popoverPresentationController {
                        self.popoverController = popoverController
                        popoverController.sourceView = self.view
                        popoverController.sourceRect = CGRect(x: self.view.bounds.midX, y: self.view.bounds.midY, width: 0, height: 0)
                        popoverController.permittedArrowDirections = []
                    }
                }
                break;
                
            case "openUrlBlank":
                guard let url = body["param"] as? String else { return }
                self.openUrlBlank(url: url)
                break
                
            case "purchase":
                guard let orderType = body["param"] as? String else { return }
                
                var productId = StoreKitManager.shared.monthProductId.first
                if orderType == "month" {
                    productId = StoreKitManager.shared.monthProductId.first
                } else if orderType == "year" {
                    productId = StoreKitManager.shared.yearProductId.first
                }
                
                if let productId = productId {
                    self.showLoading()
                    StoreKitManager.shared.purchase(productId: productId) { purchase, errorMsg in
                        if let errorMsg = errorMsg {
                            // 결제 실패
                            self.hideLoading()
                            
                            DispatchQueue.main.async {
                                Utils.shared.showSnackBar(view: self.view, message: errorMsg)
                            }
                            return
                        }
                        
                        self.hideLoading()
                        
                        if let purchase = purchase {
                            //결제 정보 넘어옴
                            print("Purchase Success: \(purchase.productId) \(purchase.originalPurchaseDate) ---- \(purchase)")
                            if let originalTransactionId = purchase.transaction.transactionIdentifier {
                                print("purchase.transaction.transactionState \(purchase.transaction.transactionState)")
                                switch purchase.transaction.transactionState {
                                case .purchased, .restored:
                                    if purchase.needsFinishTransaction {
                                        // Deliver content from server, then:
                                        SwiftyStoreKit.finishTransaction(purchase.transaction)
                                    }
                                    // Unlock content
                                case .failed, .purchasing, .deferred:
                                    break // do nothing
                                default:
                                    break
                                }
                                
                                //print("originalTransactionId \(originalTransactionId)")
                                //print("productId \(purchase.productId)")
                                self.showLoading()
                                StoreKitManager.shared.fetchReceipt { encryptedReceipt, error in
                                    if let error = error {
                                        self.hideLoading()
                                        
                                        DispatchQueue.main.async {
                                            Utils.shared.showSnackBar(view: self.view, message: error)
                                        }
                                        return
                                    }
                                    // orderid = originalTransactionId
                                    // productid = purchase.productId
                                    // token = encryptedReceipt
                                    let mt_idx = Utils.shared.getMtIdx()
                                    let token = encryptedReceipt ?? String()
                                    
                                    self.hideLoading()
                                    
                                    DispatchQueue.main.async {
                                        self.web_view.evaluateJavaScript("f_member_receipt_done_ios('\(originalTransactionId)', '\(purchase.productId)', '\(token)', '\(mt_idx)');") { (any, err) -> Void in
                                            print(err ?? "[f_member_receipt_done_ios] IOS >> 자바스크립트 : SUCCESS")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                break
                
            case "purchaseCheck":
                StoreKitManager.shared.fetchReceipt { encryptedReceipt, error in
                    let mt_idx = Utils.shared.getMtIdx()
                    
                    if let error = error {
                        print("purchaseCheck - \(error)")
                        DispatchQueue.main.async {
                            self.web_view.evaluateJavaScript("f_member_receipt_check_ios('', '\(mt_idx)');") { (any, err) -> Void in
                                print(err ?? "[f_member_receipt_check_ios] IOS >> 자바스크립트 : SUCCESS")
                            }
                        }
                        return
                    }
                    
                    let token = encryptedReceipt ?? String()
                    DispatchQueue.main.async {
                        self.web_view.evaluateJavaScript("f_member_receipt_check_ios('\(token)', '\(mt_idx)');") { (any, err) -> Void in
                            print(err ?? "[f_member_receipt_check_ios] IOS >> 자바스크립트 : SUCCESS")
                        }
                    }
                }
                break
                    
            case "restorePurchase":
                self.showLoading()
                StoreKitManager.shared.restorePurchases { msg in
                    if msg != nil {
                        StoreKitManager.shared.fetchReceipt { encryptedReceipt, error in
                            if let error = error {
                                self.hideLoading()
                                DispatchQueue.main.async {
                                    Utils.shared.showSnackBar(view: self.view, message: error)
                                }
                                return
                            }
                            
                            self.hideLoading()
                            let mt_idx = Utils.shared.getMtIdx()
                            let token = encryptedReceipt ?? String()
                            
                            DispatchQueue.main.async {
                                self.web_view.evaluateJavaScript("f_member_receipt_restore_ios('\(token)', '\(mt_idx)');") { (any, err) -> Void in
                                    print(err ?? "[location_refresh] IOS >> 자바스크립트 : SUCCESS")
                                }
                            }
                        }
                    } else {
                        self.hideLoading()
                        DispatchQueue.main.async {
                            Utils.shared.showSnackBar(view: self.view, message: "구독 복원에 실패했습니다.")
                        }
                    }
                }
                break
            case "session_refresh":
                guard let session_refresh_event_url = body["param"] as? String else { return }
                let location = LocationService.sharedInstance.getLastLocation()
                var urlString = Http.shared.WEB_BASE_URL + "auth?mt_token_id=%@"
            
                if location.coordinate.latitude != 0.0 && location.coordinate.longitude != 0.0 {
                    urlString = "\(urlString)&mt_lat=\(location.coordinate.latitude)&mt_long=\(location.coordinate.longitude)"
                }
                
                Utils.shared.getToken { token in
                    urlString = String.init(format: urlString, token)
                    urlString += "&event_url=\(session_refresh_event_url)"
                    print("url String == \(urlString)")
                    
                    let url = URL(string: urlString)
                    var request = URLRequest(url: url!)
                    request.setValue(Http.shared.hashKey, forHTTPHeaderField: "AUTH_SECRETKEY")
                    self.web_view.load(request)
                }
                break
            case "showAd":
                self.showAd()
                break
//            case "keyboard":
//                UIApplication.shared.sendAction(#selector(UIView.resignFirstResponder), to: nil, from: nil, for: nil)
//                DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
//                    self.web_view.becomeFirstResponder()
//                    self.web_view.evaluateJavaScript("f_keyboard_open();") { (any, err) -> Void in
//                        print(err ?? "[f_keyboard_open] IOS >> 자바스크립트 : SUCCESS")
//                    }
//               }
//                break
            default:
                break
            }
        }
    }
    
    public func webViewWebContentProcessDidTerminate(_ webView: WKWebView) {
        print("webViewWebContentProcessDidTerminate")
        webView.reload()
    }
    
//    func webViewDidClose(_ webView: WKWebView) {
//        print("webViewDidClose")
//        //webView.removeFromSuperview()
//    }
}

extension MainView: MFMessageComposeViewControllerDelegate {
    func messageComposeViewController(_ controller: MFMessageComposeViewController, didFinishWith result: MessageComposeResult) {
        controller.dismiss(animated: false)
    }
}

extension MainView: YPImagePickerDelegate {
    func imagePickerHasNoItemsInLibrary(_ picker: YPImagePicker) {
        Utils.shared.showSnackBar(view: self.view, message: "가져올 수 있는 사진이 없습니다.")
    }

    func shouldAddToSelection(indexPath: IndexPath, numSelections: Int) -> Bool {
        // false 로 설정하면 선택해도 다음으로 갈 수 없다. 즉, 추가할 수 없음.
        return true
    }
}

extension MainView: GADFullScreenContentDelegate {
    /// Tells the delegate that the ad failed to present full screen content.
    func ad(_ ad: GADFullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        print("Ad did fail to present full screen content.")
        self.web_view.evaluateJavaScript("failAd('show');") { (any, err) -> Void in
            print(err ?? "[failAd] IOS >> 자바스크립트 : SUCCESS")
        }
    }

    /// Tells the delegate that the ad will present full screen content.
    func adWillPresentFullScreenContent(_ ad: GADFullScreenPresentingAd) {
        print("Ad will present full screen content.")
    }

    /// Tells the delegate that the ad dismissed full screen content.
    func adDidDismissFullScreenContent(_ ad: GADFullScreenPresentingAd) {
        print("Ad did dismiss full screen content.")
        // 닫힐때
        self.web_view.evaluateJavaScript("endAd();") { (any, err) -> Void in
            print(err ?? "[endAd] IOS >> 자바스크립트 : SUCCESS")
            Task {
                await self.loadAds(isShow:false, errorCount:0)
            }
        }
    }
}
