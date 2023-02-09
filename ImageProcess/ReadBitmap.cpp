#include <windows.h>
#include "stdafx.h"
#include "ReadBitmap.h"
#include <opencv2\core.hpp>
#include <opencv2\imgproc.hpp>
#include <opencv2\highgui.hpp>
//#include <opencv2\features2d.hpp>
//#include <opencv2\calib3d.hpp>
//#include <string>

using namespace cv;


ImageProcess::ReadBitmap::ReadBitmap()
{

}

void ImageProcess::ReadBitmap::getByte()
{
	Mat test2 = imread("C:\\Users\\junym\\source\\repos\\IMT\\\ImageProcess\\Sec07_ICC Merged_RAW_ch00.tif", CV_LOAD_IMAGE_ANYDEPTH);
	imwrite("C:\\Users\\junym\\source\\repos\\IMT\\\ImageProcess\\MyImage.jpg", test2);
}
//