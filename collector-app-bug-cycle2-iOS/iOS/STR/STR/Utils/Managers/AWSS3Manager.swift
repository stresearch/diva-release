//
//  AWSS3Manager.swift
//  STR
//
//  Created by Srujan on 14/02/20.
//  
//

import Foundation
import UIKit
import AWSS3 //1
import AWSMobileClient

typealias progressBlock = (_ progress: Double) -> Void
typealias completionBlock = (_ response: Any?, _ error: Error?) -> Void
typealias completionBlockDownload = (_ success: Bool?, _ error: Error?) -> Void

class AWSS3Manager {
    
    static let shared = AWSS3Manager()
    private init () { }
    
    
    @objc lazy var transferUtility = {
        AWSS3TransferUtility.default()
    }()
    
    
    // Upload image using UIImage object
    func uploadImage(image: UIImage, progress: progressBlock?, completion: completionBlock?) {
        
        guard let imageData = image.jpegData(compressionQuality: 1.0) else {
            let error = NSError(domain:"", code:402, userInfo:[NSLocalizedDescriptionKey: "invalid image"])
            completion?(nil, error)
            return
        }
        
        let tmpPath = NSTemporaryDirectory() as String
        let fileName: String = UUID().uuidString + (".jpeg")
        let filePath = tmpPath + "/" + fileName
        let fileUrl = URL(fileURLWithPath: filePath)
        
        do {
            try imageData.write(to: fileUrl)
            self.uploadfile(fileUrl: fileUrl, fileName: fileName, contenType: "image", progress: progress, completion: completion)
        } catch {
            let error = NSError(domain:"", code:402, userInfo:[NSLocalizedDescriptionKey: "invalid image"])
            completion?(nil, error)
        }
    }
    
    // Upload video from local path url
    func uploadVideo(videoUrl: URL, fileName: String, progress: progressBlock?, completion: completionBlock?) {
        let fileName = self.getUniqueFileName(fileUrl: videoUrl, fileName: fileName)
        self.uploadfile(fileUrl: videoUrl, fileName: fileName, contenType: "video", progress: progress, completion: completion)
    }
    
    // Upload auido from local path url
    func uploadAudio(audioUrl: URL, fileName: String, progress: progressBlock?, completion: completionBlock?) {
        let fileName = self.getUniqueFileName(fileUrl: audioUrl, fileName: fileName)
        self.uploadfile(fileUrl: audioUrl, fileName: fileName, contenType: "audio", progress: progress, completion: completion)
    }
    
    // Upload files like Text, Zip, etc from local path url
    func uploadOtherFile(fileUrl: URL, fileName: String, conentType: String, progress: progressBlock?, completion: completionBlock?) {
        let fileName = self.getUniqueFileName(fileUrl: fileUrl, fileName: fileName)
        self.uploadfile(fileUrl: fileUrl, fileName: fileName, contenType: conentType, progress: progress, completion: completion)
    }
    
    // Upload Using Multipart files like Text, Zip, etc from local path url to
    func uploadMultipartOtherFile(fileUrl: URL, fileName: String, conentType: String, progress: progressBlock?, completion: completionBlock?) {
        let fileName = self.getUniqueFileName(fileUrl: fileUrl, fileName: fileName)
        self.uploadMultipartfile(fileUrl: fileUrl, fileName: fileName, contenType: conentType, progress: progress, completion: completion)
    }
    
    // Get unique file name
    func getUniqueFileName(fileUrl: URL, fileName: String) -> String {
        let strExt: String = "." + (URL(fileURLWithPath: fileUrl.absoluteString).pathExtension)
        return (fileName + (strExt))
    }
    
    //MARK:- AWS file upload
    // fileUrl :  file local path url
    // fileName : name of file, like "myimage.jpeg" "video.mov"
    // contenType: file MIME type
    // progress: file upload progress, value from 0 to 1, 1 for 100% complete
    // completion: completion block when uplaoding is finish, you will get S3 url of upload file here
    
    
    private func uploadfile(fileUrl: URL, fileName: String, contenType: String, progress: progressBlock?, completion: completionBlock?) {
        // Upload progress block
        //let name = "public/" + (AWSMobileClient.default().identityId ?? "") + "/\(fileName)"
        let expression = AWSS3TransferUtilityUploadExpression()
        expression.progressBlock = {(task, awsProgress) in
            guard let uploadProgress = progress else { return }
            DispatchQueue.main.async {
                uploadProgress(awsProgress.fractionCompleted)
            }
        }
        // Completion block
        var completionHandler: AWSS3TransferUtilityUploadCompletionHandlerBlock?
        completionHandler = { (task, error) -> Void in
            DispatchQueue.main.async(execute: {
                if error == nil {
                    //let url = AWSS3.default().configuration.endpoint.url
                    //let publicURL = url?.appendingPathComponent(self.bucketName).appendingPathComponent(fileName)
                    let publicURL = LocalizableString.s3BucketUrl.rawValue + fileName
                    print("Uploaded to:\(String(describing: publicURL))")
                    if let completionBlock = completion {
                        completionBlock(publicURL, nil)
                    }
                } else {
                    if let completionBlock = completion {
                        completionBlock(nil, error)
                    }
                }
            })
        }

        // Start uploading using AWSS3TransferUtility
//        let awsTransferUtility = AWSS3TransferUtility.default()
        let awsTransferUtility = transferUtility

        awsTransferUtility.uploadFile(fileUrl, bucket: AppDelegate.s3BucketName ?? "", key: fileName, contentType: contenType, expression: expression, completionHandler: completionHandler).continueWith { (task) -> Any? in
            if let error = task.error {
                print("error is: \(error.localizedDescription)")
            }
            if let _ = task.result {
                // your uploadTask
            }
            return nil
        }
    }
    
    //Multi Part
    private func uploadMultipartfile(fileUrl: URL, fileName: String, contenType: String, progress: progressBlock?, completion: completionBlock?) {
            // Upload progress block
        let expression = AWSS3TransferUtilityMultiPartUploadExpression()
        
        var valProgressBlock: AWSS3TransferUtilityMultiPartProgressBlock?
        valProgressBlock = {(task, progress) in
            DispatchQueue.main.async(execute: {
                print("progressBlock---\(progress.fractionCompleted)")
                
            })
        }
        expression.progressBlock = valProgressBlock
        

        var completeionHandler: AWSS3TransferUtilityMultiPartUploadCompletionHandlerBlock?

        completeionHandler = { (task, error) in
            DispatchQueue.main.async(execute: {
                if error == nil {
                    let url = AWSS3.default().configuration.endpoint.url
                    let publicURL = url?.appendingPathComponent(AppDelegate.s3BucketName ?? "").appendingPathComponent(fileName)
                    print("Uploaded to:\(String(describing: publicURL))")
                    if let completionBlock = completion {
                        completionBlock(publicURL?.absoluteString, nil)
                    }
                } else {
                    if let completionBlock = completion {
                        completionBlock(nil, error)
                    }
                }
            })
        }

        let awsTransferUtility = transferUtility
        
        awsTransferUtility.uploadUsingMultiPart(fileURL: fileUrl, bucket: AppDelegate.s3BucketName ?? "", key: fileName, contentType: contenType, expression: expression, completionHandler: completeionHandler).continueWith { (task) -> Any? in
            
            if let error = task.error {
                print("error is: \(error.localizedDescription)")
            }
            if let _ = task.result {
                // your uploadTask
            }
            return nil
        }
        
        }
    
    func downloadFile(fileUrlString: String, fileName: String, bucketName: String, completionHandler: completionBlockDownload?) {
        let expression = AWSS3TransferUtilityDownloadExpression()
        
        let transferUtility = AWSS3TransferUtility.default()
        print("outputVideoPath---\(fileUrlString)")
        
        
        transferUtility.download(to: URL(fileURLWithPath: fileUrlString), bucket: bucketName, key: fileName, expression: expression, completionHandler: { (task, URL, data, error) in
            
            DispatchQueue.main.async {
                if error != nil {
                    print("error---\(error!)")
                    if let completionBlock = completionHandler {
                        completionBlock(false, error)
                    }
                }
                
                if let completionBlock = completionHandler {
                    completionBlock(true, error)
                }
            }            
        })
    }
    
}

