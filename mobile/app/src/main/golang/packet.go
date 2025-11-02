package main

import (
	"encoding/binary"
	"net"
)

type IPPacket struct {
	Version  uint8
	Protocol uint8
	SrcIP    net.IP
	DstIP    net.IP
	SrcPort  uint16
	DstPort  uint16
	Payload  []byte
}

func parseIPv4Packet(data []byte) (*IPPacket, error) {
	if len(data) < 20 {
		return nil, nil
	}
	
	packet := &IPPacket{
		Version:  data[0] >> 4,
		Protocol: data[9],
	}
	
	headerLen := int(data[0]&0x0F) * 4
	if len(data) < headerLen {
		return nil, nil
	}
	
	packet.SrcIP = net.IP(data[12:16])
	packet.DstIP = net.IP(data[16:20])
	
	if len(data) > headerLen {
		transportData := data[headerLen:]
		
		switch packet.Protocol {
		case 6: // TCP
			if len(transportData) >= 4 {
				packet.SrcPort = binary.BigEndian.Uint16(transportData[0:2])
				packet.DstPort = binary.BigEndian.Uint16(transportData[2:4])
			}
		case 17: // UDP
			if len(transportData) >= 4 {
				packet.SrcPort = binary.BigEndian.Uint16(transportData[0:2])
				packet.DstPort = binary.BigEndian.Uint16(transportData[2:4])
			}
		}
		
		packet.Payload = data
	}
	
	return packet, nil
}

func parseIPv6Packet(data []byte) (*IPPacket, error) {
	if len(data) < 40 {
		return nil, nil
	}
	
	packet := &IPPacket{
		Version:  data[0] >> 4,
		Protocol: data[6],
	}
	
	packet.SrcIP = net.IP(data[8:24])
	packet.DstIP = net.IP(data[24:40])
	
	if len(data) > 40 {
		transportData := data[40:]
		
		switch packet.Protocol {
		case 6: // TCP
			if len(transportData) >= 4 {
				packet.SrcPort = binary.BigEndian.Uint16(transportData[0:2])
				packet.DstPort = binary.BigEndian.Uint16(transportData[2:4])
			}
		case 17: // UDP
			if len(transportData) >= 4 {
				packet.SrcPort = binary.BigEndian.Uint16(transportData[0:2])
				packet.DstPort = binary.BigEndian.Uint16(transportData[2:4])
			}
		}
		
		packet.Payload = data
	}
	
	return packet, nil
}



