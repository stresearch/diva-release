//
//  UIViewController+Extensions.swift
//  STR
//
//  Created by Srujan on 30/07/19.
//  
//

import SVProgressHUD
import UIKit

extension UIViewController {
    
    func topMostViewController() -> UIViewController {
        
        if self.presentedViewController == nil {
            return self
        }
        if let navigation = self.presentedViewController as? UINavigationController {
            return navigation.visibleViewController!.topMostViewController()
        }
        if let tab = self.presentedViewController as? UITabBarController {
            if let selectedTab = tab.selectedViewController {
                return selectedTab.topMostViewController()
            }
            return tab.topMostViewController()
        }
        return self.presentedViewController!.topMostViewController()
    }
    
    func presentDefaultAlertWithTitle(
        title: String?,
        message: String?,
        animated: Bool,
        completion: (() -> Void)?
    ) {
        let alert = UIAlertController(title: title, message: message, preferredStyle: .alert)
        alert.view.tintColor = Color.main.color
        let okAction = UIAlertAction(
            title: LocalizableString.ok.localizedString,
            style: .default,
            handler: { [weak self] (_) in
                self?.view.endEditing(true)
                if let completion = completion { completion() }
            }
        )
        alert.addAction(okAction)
        present(alert, animated: animated)
    }
    
    func presentDefaultAlertWithError(
        error: ErrorPresentable,
        animated: Bool,
        completion: (() -> Void)?
    ) {
        let alert = UIAlertController(
            title: error.title,
            message: error.message,
            preferredStyle: .alert
        )
        alert.view.tintColor = Color.main.color
        let okAction = UIAlertAction(
            title: LocalizableString.ok.localizedString,
            style: .default,
            handler: nil
        )
        alert.addAction(okAction)
        present(alert, animated: animated) { [weak self] in
            self?.view.endEditing(true)
            if let completion = completion { completion() }
        }
    }
    
    func presentAlertWithTitle(
        title: String?,
        message: String?,
        animated: Bool,
        completion: (() -> Void)?,
        action: UIAlertAction
    ) {
        let alert = UIAlertController(title: title, message: message, preferredStyle: .alert)
        alert.view.tintColor = Color.main.color
        let okAction = action
        alert.addAction(okAction)
        present(alert, animated: animated) { [unowned self] in
            self.view.endEditing(true)
            if let completion = completion { completion() }
        }
    }
    
    func showAlertWithDecision(
        title: String? = nil,
        message: String,
        successHandler: ((UIAlertAction) -> Void)?,
        completion: (() -> Void)?
    ) {
        
        let alertController = UIAlertController(
            title: title,
            message: message,
            preferredStyle: .alert
        )
        alertController.view.tintColor = Color.main.color
        
        let noAction = UIAlertAction(
            title: LocalizableString.no.localizedString,
            style: .cancel,
            handler: nil
        )
        
        let yesAction = UIAlertAction(
            title: LocalizableString.yes.localizedString,
            style: .default,
            handler: successHandler
        )
        
        alertController.addAction(noAction)
        alertController.addAction(yesAction)
        
        present(alertController, animated: true, completion: completion)
    }
    
    func showTextFieldAlertWith(
        title: String? = nil,
        message: String,
        leftImage: UIImage? = nil,
        keyboardType: UIKeyboardType = UIKeyboardType.default,
        placeholder: String,
        successHandler: ((String) -> Void)?,
        completion: (() -> Void)?
    ) {
        
        let alert = UIAlertController(style: .actionSheet, title: title, message: message)
        
        var zeroValue = ""
        
        let textField: TextField.Config = { textField in
            if let image = leftImage { textField.left(image: image, color: .black) }
            textField.leftViewPadding = 12
            textField.becomeFirstResponder()
            textField.borderWidth = 1
            textField.cornerRadius = 8
            textField.borderColor = UIColor.lightGray.withAlphaComponent(0.5)
            textField.backgroundColor = nil
            textField.textColor = .black
            textField.placeholder = placeholder
            textField.keyboardAppearance = .default
            textField.keyboardType = keyboardType
            textField.returnKeyType = .done
            textField.action { textField in
                zeroValue = textField.text ?? ""
            }
        }
        
        alert.addOneTextField(configuration: textField)
        alert.addAction(title: LocalizableString.ok.localizedString) { (_) in
            successHandler?(zeroValue)
        }
        alert.show()
    }
    
    func showTextViewAlertWith(
        title: String? = nil,
        message: String,
        keyboardType: UIKeyboardType = UIKeyboardType.default,
        placeholder: String,
        successHandler: ((String) -> Void)?,
        completion: (() -> Void)?
    ) {
        
        let alert = UIAlertController(style: .actionSheet, title: title, message: message)
        var zeroValue = ""
        
        alert.addTextViewer(placeholder: placeholder) { (value) in
            zeroValue = value
        }
        alert.addAction(title: LocalizableString.ok.localizedString) { (_) in
            successHandler?(zeroValue)
        }
        alert.show()
    }
    
    func loadImageDataFrom(url: URL, completion: ((Data?) -> Void)?) {
        DispatchQueue.global().async {
            let data = try? Data(contentsOf: url)
            DispatchQueue.main.async { completion?(data) }
        }
    }
    
    func showProgress() {
        self.view.isUserInteractionEnabled = false
        SVProgressHUD.show()
    }
    
    func hideProgress() {
        self.view.isUserInteractionEnabled = true
        SVProgressHUD.dismiss()
    }
    
    func showSuccesWith(message: String?) { SVProgressHUD.showSuccess(withStatus: message) }
    
}

