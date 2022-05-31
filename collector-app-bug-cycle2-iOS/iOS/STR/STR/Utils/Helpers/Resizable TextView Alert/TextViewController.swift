import UIKit

extension UIAlertController {

    /// Add a Text Viewer
    ///
    /// - Parameters:
    ///   - text: string
    ///   - placeholder: string

    func addTextViewer(placeholder: String, action: @escaping (String) -> Void) {
        let textViewer = TextViewerViewController(placeholder: placeholder)
        textViewer.add(action: action)

        let height: CGFloat = TextViewerViewController.UI.height + TextViewerViewController.UI
            .vInset
        set(vc: textViewer, height: height)
    }
}

final class TextViewerViewController: UIViewController {

    public typealias Action = (String) -> Void
   

    fileprivate lazy var textView: UITextView = {
        $0.isEditable = true
        $0.isSelectable = true
        $0.backgroundColor = nil
        $0.textColor = UIColor.lightGray
        $0.font = UIFont.systemFont(ofSize: 15)
        return $0
    }(UITextView())

    struct UI {
        static let height: CGFloat = 30
        static let vInset: CGFloat = 16
        static let hInset: CGFloat = 16
    }

    private lazy var placeholder = "Enter your reason here"
    fileprivate var actionEditingChanged: Action?

    init(placeholder: String) {
        super.init(nibName: nil, bundle: nil)

        textView.text = placeholder
        self.placeholder = placeholder
        textView.textContainerInset = UIEdgeInsets(
            top: 0,
            left: UI.vInset,
            bottom: UI.hInset,
            right: UI.vInset
        )
    }

    public func add(action: @escaping Action) {
        self.textView.delegate = self
        self.actionEditingChanged = action
    }

    required init?(coder aDecoder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    deinit { Log("has deinitialized") }

    override func loadView() { view = textView }

    override func viewDidLoad() {
        super.viewDidLoad()

        if UIDevice.current.userInterfaceIdiom == .pad {
            preferredContentSize.width = UIScreen.main.bounds.width * 0.618
        }

    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        textView.becomeFirstResponder()
        textView.selectedRange = NSRange(location: 0, length: 0)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        let contentSizeHeight = textView.contentSize.height
        var height = contentSizeHeight > UI.height ? contentSizeHeight : UI.height
        let maxHeight = UIScreen.main.bounds.height * 0.3
        height = height > maxHeight ? maxHeight : height
        preferredContentSize.height = height
    }

}

extension TextViewerViewController: UITextViewDelegate {

    func textViewDidChangeSelection(_ textView: UITextView) {
        // Move cursor to beginning on first tap
        if textView.text == placeholder { textView.selectedRange = NSRange(location: 0, length: 0) }
    }

    func textView(
        _ textView: UITextView,
        shouldChangeTextIn range: NSRange,
        replacementText text: String
    ) -> Bool {
        if textView.text == placeholder && !text.isEmpty {
            textView.text = nil
            textView.textColor = UIColor.black
            textView.selectedRange = NSRange(location: 0, length: 0)
        }
        return true
    }

    func textViewDidChange(_ textView: UITextView) {
        if textView.text.isEmpty {
            textView.textColor = UIColor.lightGray
            textView.text = placeholder
        } else { actionEditingChanged?(textView.text) }
    }

    func textViewDidEndEditing(_ textView: UITextView) {
        self.view.layoutSubviews()
        self.view.layoutIfNeeded()
    }

}
