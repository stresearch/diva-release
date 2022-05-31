//
//  ChangeUserNameView.swift
//  STR
//
//  Created by Srujan on 03/02/20.
//  
//

import UIKit

enum ChangeUserName: TableRow {
    
    case newEmail, confirmEmail, enterPassword
    
    var title: String {
        switch self {
        case .newEmail:
            return LocalizableString.newEmail.localizedString
        case .confirmEmail:
            return LocalizableString.confirmEmail.localizedString
        case .enterPassword:
            return LocalizableString.enterPassword.localizedString
        }
    }
}

enum ChangePassword: TableRow {
    
    case currentPassword, newPassword, confirmPassword
    
    var title: String {
        switch self {
        case .currentPassword:
            return LocalizableString.currentPassword.localizedString
        case .newPassword:
            return LocalizableString.newPassword.localizedString
        case .confirmPassword:
            return LocalizableString.confirmPassword.localizedString
        }
    }
}

protocol ChangeUserNameViewDelegate: class {
    func didChangeTextfieldValue(for row: TableRow, value: String)
    func didTapOnSubmit()
    func didTapOnCancel()
}

class ChangeUserNameView: UIView {

    //MARK: Outlets
    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var textfield1: TextField!
    @IBOutlet weak var textfield2: TextField!
    @IBOutlet weak var textfield3: TextField!
    @IBOutlet weak var viewbackground: UIView!
    
    //MARK: Data members
    var isUsername: Bool! {
        didSet {
            if isUsername {
                labelTitle.text = LocalizableString.changeUsername.localizedString
                textfield1.placeholder = LocalizableString.newEmail.localizedString
                textfield2.placeholder = LocalizableString.confirmEmail.localizedString
                textfield3.placeholder = LocalizableString.enterPassword.localizedString
                textfield3.isSecureTextEntry = true
            } else {
                labelTitle.text = LocalizableString.changePassword.localizedString
                textfield1.placeholder = LocalizableString.currentPassword.localizedString
                textfield2.placeholder = LocalizableString.newPassword.localizedString
                textfield3.placeholder = LocalizableString.confirmPassword.localizedString
                textfield1.isSecureTextEntry = true
                textfield2.isSecureTextEntry = true
                textfield3.isSecureTextEntry = true
            }
        }
    }
    
    weak var delegate: ChangeUserNameViewDelegate?

    deinit { Log("\(self) I'm gone ") }

    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        
    }

    override init(frame: CGRect) {
        super.init(frame: frame)
        
    }

    class func instanceFromNib() -> ChangeUserNameView? {
        let view = UINib(nibName: "ChangeUserNameView", bundle: nil).instantiate(
          withOwner: nil,
          options: nil
        ).first as? ChangeUserNameView
        return view
    }
    
    func addTextFieldTarget() {
        textfield1.addTarget(self, action: #selector(didChangeEditing(_:)), for: .editingChanged)
        textfield2.addTarget(self, action: #selector(didChangeEditing(_:)), for: .editingChanged)
        textfield3.addTarget(self, action: #selector(didChangeEditing(_:)), for: .editingChanged)
    }
    
    //MARK: Actions
    @IBAction func saveBtnAction(_ sender: UIButton) {
        delegate?.didTapOnSubmit()
    }
    
    @IBAction func cancelBtnAction(_ sender: UIButton) {
        delegate?.didTapOnCancel()
    }
    
    @objc private func didChangeEditing(_ sender: UITextField) {
        if let value = sender.text {
            switch sender.tag {
            case 1:
                delegate?.didChangeTextfieldValue(for: isUsername ? ChangeUserName.newEmail : ChangePassword.currentPassword, value: value)
            case 2:
                delegate?.didChangeTextfieldValue(for: isUsername ? ChangeUserName.confirmEmail: ChangePassword.newPassword, value: value)
            case 3:
                delegate?.didChangeTextfieldValue(for: isUsername ? ChangeUserName.enterPassword : ChangePassword.confirmPassword, value: value)
            default:
                break
            }
        }
    }
}
