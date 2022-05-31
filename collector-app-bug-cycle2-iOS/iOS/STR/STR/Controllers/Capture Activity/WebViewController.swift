//
//  WebViewController.swift
//  STR
//
//  Created by Srujan on 06/02/20.
//  
//

import UIKit
import WebKit

enum WebViewType {
    case url
    case html
}

class WebViewController: UIViewController {

    //MARK: Outlets
    @IBOutlet weak var wkWebView: WKWebView!
    @IBOutlet weak var activity: UIActivityIndicatorView!
    
    var moreInfo: String!
    var webViewType: WebViewType = .url
    
    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        activity.startAnimating()
        wkWebView.navigationDelegate = self
        activity.hidesWhenStopped = true
        
        if #available(iOS 13.0, *) {
            activity.style = .medium
        } else {
            // Fallback on earlier versions
            activity.style = .gray
        }
        
        switch webViewType {
        case .html:
            loahHtmlString()
        case .url:
            loadUrl()
        }
        
    }

    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destination.
        // Pass the selected object to the new view controller.
    }
    */

    private func loadHtmlUrlFromLocal() ->  URL? {
        if let path = Utilities.getFilePathFromLocal(fileName: "ConsentMoreInfo", type: "html") {
            return URL(fileURLWithPath: path)
        }
        return nil
    }
    
    private func loadUrl() {
        wkWebView.load(URLRequest(url: URL(string: moreInfo)!))
    }
    
    private func loahHtmlString() {
        let headerString = "<header><meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no'></header>"
        wkWebView.loadHTMLString(headerString + moreInfo, baseURL: nil)
    }
    
    //MARK: Actions
    
    @IBAction func closeBtnAction(_ sender: UIButton) {
        self.dismiss(animated: true, completion: nil)
    }
    
}

//MARK: WKWebView Delegate Methods
extension WebViewController: WKNavigationDelegate {
    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        activity.stopAnimating()
    }

    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        activity.stopAnimating()
    }
}
