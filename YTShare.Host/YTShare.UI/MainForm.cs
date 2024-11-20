using System.Net;
using System.Net.Sockets;

namespace YTShare.UI;

public partial class MainForm : Form
{
    public MainForm()
    {
        InitializeComponent();
    }

    private void pictureBox1_Click(object sender, EventArgs e)
    {

    }

    private void label1_Click(object sender, EventArgs e)
    {

    }

    private void MainForm_Load(object sender, EventArgs e)
    {
        DisplayIpAddress();
    }

    private void DisplayIpAddress()
    {
        string ipAddress = GetLocalIPAddress();
        lblIpAddress.Text = ipAddress;
        GenerateQrCode(ipAddress);
    }

    private string GetLocalIPAddress()
    {
        var host = Dns.GetHostEntry(Dns.GetHostName());
        foreach (var ip in host.AddressList)
        {
            if (ip.AddressFamily == AddressFamily.InterNetwork && ip == host.AddressList[5])
            {
                return ip.ToString();
            }
        }
        throw new Exception("Local IP Address Not Found!");
    }
    private void GenerateQrCode(string ipAddress)
    {
        using (var qrGenerator = new QRCoder.QRCodeGenerator())
        using (var qrCodeData = qrGenerator.CreateQrCode(ipAddress, QRCoder.QRCodeGenerator.ECCLevel.Q))
        using (var qrCode = new QRCoder.QRCode(qrCodeData))
        {
            picQrCode.Image = qrCode.GetGraphic(10);
        }
    }

}
