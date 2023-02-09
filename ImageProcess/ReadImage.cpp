#include "stdafx.h"

#include "ReadImage.h"

#include <opencv2\core.hpp>
#include <opencv2\imgproc.hpp>
#include <opencv2\highgui.hpp>
#include <opencv2\features2d.hpp>
#include <opencv2\calib3d.hpp>

using namespace cv;
using namespace System::Runtime::InteropServices;
using namespace System::Collections::Generic;

namespace ImageProcess
{
	

	void dev()
	{
		Mat srcImage;
		srcImage = imread("C:/Users/junym/Desktop/Test_composites/1 100x.1.jpg");
		imshow("test", srcImage);
		waitKey(0);
		
	}

}

int ImageProcess::ReadImage::test(int a)
{
	
	a = 6;
	return a;
}



//int main()
//{
//	Mat srcImage;
//	cout << 1 << endl;
//	srcImage = imread("C:/Users/junym/Desktop/Test_composites/1 100x.1.jpg");
//	imshow("test", srcImage);
//	waitKey(0);
//	return 0;
//}
