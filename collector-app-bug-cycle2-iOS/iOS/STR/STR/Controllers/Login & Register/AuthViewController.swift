//
//  AuthViewController.swift
//  STR
//
//  Created by Srujan on 31/12/19.
//  
//

import UIKit

class AuthViewController: UIViewController {

    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
    }
    
    override func viewWillAppear(_ animated: Bool) {
        self.navigationController?.navigationBar.isHidden = true
    }

    @IBAction func signInBtnPressed(_ sender: UIButton) {
        guard let signInVC = self.storyboard?
            .instantiateViewController(withIdentifier: SignInViewController.storyboardIdentifier)
            else { return }
        self.navigationController?.pushViewController(signInVC, animated: true)
    }

    @IBAction func signUpBtnPressed(_ sender: UIButton) {
        guard let signUpVC = self.storyboard?
            .instantiateViewController(withIdentifier: SignUpViewController.storyboardIdentifier)
            else { return }
        self.navigationController?.pushViewController(signUpVC, animated: true)
    }

}
